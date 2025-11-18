package com.riverflow.service.mindmap;

import com.riverflow.dto.collaboration.InviteCollaboratorRequest;
import com.riverflow.exception.mindmap.MindmapAccessDeniedException;
import com.riverflow.exception.mindmap.MindmapNotFoundException;
import com.riverflow.model.User;
import com.riverflow.model.mindmap.CollaborationInvitation;
import com.riverflow.model.mindmap.Mindmap;
import com.riverflow.model.mindmap.subdocuments.Collaborator;
import com.riverflow.repository.UserRepository;
import com.riverflow.repository.mindmap.CollaborationInvitationRepository;
import com.riverflow.repository.mindmap.MindmapRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollaborationService {

    private final MindmapRepository mindmapRepository;
    private final CollaborationInvitationRepository invitationRepository;
    private final UserRepository userRepository;

    /**
     * Mời cộng tác viên mới
     */
    @Transactional
    public CollaborationInvitation inviteCollaborator(String mindmapId, InviteCollaboratorRequest request, Long ownerId) {
        log.info("User {} đang mời {} vào mindmap {}", ownerId, request.getEmail(), mindmapId);

        Mindmap mindmap = mindmapRepository.findById(mindmapId)
                .orElseThrow(() -> new MindmapNotFoundException(mindmapId, ownerId));

        if (!mindmap.getMysqlUserId().equals(ownerId)) {
            throw new MindmapAccessDeniedException("Chỉ chủ sở hữu mới có quyền mời cộng tác viên.", mindmapId, ownerId);
        }

        String emailToInvite = request.getEmail().trim().toLowerCase();

        boolean isAlreadyMember = mindmap.getCollaborators().stream()
                .anyMatch(c -> {
                    // Nếu collaborator đã lưu email (nếu có) hoặc so sánh qua ID
                    // Ở đây tạm thời ta cần logic map User ID -> Email hoặc ngược lại.
                    // Cách đơn giản nhất: Kiểm tra trong bảng User xem email đó có ID là gì
                    return false; // (Logic phức tạp hơn sẽ xử lý sau, tạm thời bỏ qua check này hoặc làm kỹ hơn ở dưới)
                });

        User invitedUser = userRepository.findByEmail(emailToInvite).orElse(null);
        Long invitedUserId = (invitedUser != null) ? invitedUser.getId() : null;

        if (invitedUserId != null) {
            boolean exists = mindmap.getCollaborators().stream()
                    .anyMatch(c -> c.getMysqlUserId().equals(invitedUserId));

            if (exists || mindmap.getMysqlUserId().equals(invitedUserId)) {
                throw new IllegalArgumentException("Người dùng này đã là thành viên của mindmap.");
            }
        }

        if (invitationRepository.findByMindmapIdAndInvitedEmailAndStatus(mindmapId, emailToInvite, "pending").isPresent()) {
            throw new IllegalArgumentException("Đã có lời mời đang chờ xác nhận gửi tới email này.");
        }

        String token = UUID.randomUUID().toString();

        CollaborationInvitation invitation = CollaborationInvitation.builder()
                .mindmapId(mindmapId)
                .invitedByUserId(ownerId)
                .invitedEmail(emailToInvite)
                .invitedUserId(invitedUserId)
                .role(request.getRole())
                .status("pending")
                .token(token)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        CollaborationInvitation savedInvitation = invitationRepository.save(invitation);

        log.info("Lời mời đã được tạo. Token: {}", token);

        return savedInvitation;
    }

    /**
     * Lấy danh sách collaborators của một mindmap
     */
    public List<Collaborator> getCollaborators(String mindmapId, Long userId) {
        log.info("Getting collaborators for mindmap: {} by user: {}", mindmapId, userId);

        Mindmap mindmap = mindmapRepository.findById(mindmapId)
                .orElseThrow(() -> new MindmapNotFoundException(mindmapId, userId));

        // Kiểm tra quyền (chủ sở hữu hoặc collaborator)
        boolean hasAccess = mindmap.getMysqlUserId().equals(userId) ||
                mindmap.getCollaborators().stream()
                        .anyMatch(c -> c.getMysqlUserId().equals(userId));

        if (!hasAccess) {
            throw new MindmapAccessDeniedException("Không có quyền truy cập.", mindmapId, userId);
        }

        return mindmap.getCollaborators();
    }

    /**
     * Cập nhật quyền của collaborator
     */
    @Transactional
    public Collaborator updateCollaboratorRole(String mindmapId, String email, String role, Long userId) {
        log.info("Updating collaborator role for mindmap: {} email: {} by user: {}", mindmapId, email, userId);

        Mindmap mindmap = mindmapRepository.findById(mindmapId)
                .orElseThrow(() -> new MindmapNotFoundException(mindmapId, userId));

        if (!mindmap.getMysqlUserId().equals(userId)) {
            throw new MindmapAccessDeniedException("Chỉ chủ sở hữu mới có quyền cập nhật quyền.", mindmapId, userId);
        }

        Collaborator collaborator = mindmap.getCollaborators().stream()
                .filter(c -> c.getEmail() != null && c.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Collaborator không tìm thấy"));

        collaborator.setRole(role);
        mindmapRepository.save(mindmap);

        return collaborator;
    }

    /**
     * Xóa collaborator khỏi mindmap
     */
    @Transactional
    public void removeCollaborator(String mindmapId, String email, Long userId) {
        log.info("Removing collaborator from mindmap: {} email: {} by user: {}", mindmapId, email, userId);

        Mindmap mindmap = mindmapRepository.findById(mindmapId)
                .orElseThrow(() -> new MindmapNotFoundException(mindmapId, userId));

        if (!mindmap.getMysqlUserId().equals(userId)) {
            throw new MindmapAccessDeniedException("Chỉ chủ sở hữu mới có quyền xóa collaborator.", mindmapId, userId);
        }

        mindmap.getCollaborators().removeIf(c -> 
                c.getEmail() != null && c.getEmail().equalsIgnoreCase(email)
        );

        mindmapRepository.save(mindmap);
    }
}