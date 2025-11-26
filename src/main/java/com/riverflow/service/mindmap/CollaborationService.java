import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollaborationService {

    private final MindmapRepository mindmapRepository;
    private final CollaborationInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final SmtpEmailService smtpEmailService;
    private final RestTemplate restTemplate;

    @Value("${realtime.server.url}")
    private String realtimeServerUrl;

    /**
     * Mời cộng tác viên mới
     */
    @Transactional
    public CollaborationInvitation inviteCollaborator(String mindmapId, InviteCollaboratorRequest request,
            Long ownerId) {
        log.info("User {} đang mời {} vào mindmap {}", ownerId, request.getEmail(), mindmapId);

        Mindmap mindmap = mindmapRepository.findById(mindmapId)
                .orElseThrow(() -> new MindmapNotFoundException(mindmapId, ownerId));

        if (!mindmap.getMysqlUserId().equals(ownerId)) {
            throw new MindmapAccessDeniedException("Chỉ chủ sở hữu mới có quyền mời cộng tác viên.", mindmapId,
                    ownerId);
        }

        String emailToInvite = request.getEmail().trim().toLowerCase();

        User invitedUser = userRepository.findByEmail(emailToInvite).orElse(null);
        Long invitedUserId = (invitedUser != null) ? invitedUser.getId() : null;

        if (invitedUserId != null) {
            boolean exists = mindmap.getCollaborators().stream()
                    .anyMatch(c -> c.getMysqlUserId().equals(invitedUserId));

            if (exists || mindmap.getMysqlUserId().equals(invitedUserId)) {
                throw new IllegalArgumentException("Người dùng này đã là thành viên của mindmap.");
            }
        }

        if (invitationRepository.findByMindmapIdAndInvitedEmailAndStatus(mindmapId, emailToInvite, "pending")
                .isPresent()) {
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

        // Nếu user đã tồn tại, thêm vào danh sách collaborators ngay
        if (invitedUserId != null) {
            Collaborator collaborator = Collaborator.builder()
                    .mysqlUserId(invitedUserId)
                    .email(emailToInvite)
                    .role(request.getRole().toString())
                    .invitedBy(ownerId)
                    .invitedAt(LocalDateTime.now())
                    .status("pending")
                    .build();

            mindmap.getCollaborators().add(collaborator);
            mindmapRepository.save(mindmap);
            log.info("Collaborator {} added to mindmap {} with pending status", emailToInvite, mindmapId);
        }

        // Gửi email mời
        try {
            User ownerUser = userRepository.findById(ownerId).orElse(null);
            String inviterName = (ownerUser != null) ? ownerUser.getFullName() : "Someone";

            smtpEmailService.sendInvitationEmail(
                    emailToInvite,
                    token,
                    inviterName,
                    mindmap.getTitle());
            log.info("Invitation email sent successfully to {}", emailToInvite);
        } catch (Exception e) {
            log.error("Failed to send invitation email to {}: {}", emailToInvite, e.getMessage());
            // Không throw exception, vì lời mời đã được tạo
        }

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

        String oldRole = collaborator.getRole();
        collaborator.setRole(role);
        mindmapRepository.save(mindmap);

        // Send real-time notification
        notifyPermissionChange(mindmapId, "collaborator_role_changed", Map.of(
                "email", email,
                "userId", collaborator.getMysqlUserId(),
                "oldRole", oldRole,
                "newRole", role));

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

        // Find the collaborator in mindmap
        Collaborator collaborator = mindmap.getCollaborators().stream()
                .filter(c -> c.getEmail() != null && c.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .orElse(null);

        // Case 1: Collaborator exists in mindmap - remove regardless of status
        if (collaborator != null) {
            mindmap.getCollaborators().removeIf(c -> c.getEmail() != null && c.getEmail().equalsIgnoreCase(email));
            mindmapRepository.save(mindmap);
            log.info("Collaborator {} removed from mindmap {} (status: {})", email, mindmapId,
                    collaborator.getStatus());
        } else {
            // Case 2: Collaborator doesn't exist in mindmap but might have pending
            // invitation
            // This happens when user was invited but hasn't registered yet
            log.debug("Collaborator {} not found in mindmap {}, checking pending invitations", email, mindmapId);
        }

        // Remove pending invitation(s) for this email regardless
        var pendingInvitations = invitationRepository.findByMindmapIdAndInvitedEmailAndStatus(
                mindmapId,
                email.toLowerCase(),
                "pending");

        if (pendingInvitations.isPresent()) {
            CollaborationInvitation invitation = pendingInvitations.get();
            invitation.setStatus("cancelled");
            invitation.setUpdatedAt(LocalDateTime.now());
            invitationRepository.save(invitation);
            log.info("Pending invitation for {} in mindmap {} cancelled", email, mindmapId);
        }

        // If neither collaborator nor invitation exists, throw error
        if (collaborator == null && pendingInvitations.isEmpty()) {
            throw new IllegalArgumentException(
                    "Người dùng này không phải là collaborator hoặc không có lời mời đang chờ xác nhận.");
        }

        // Send real-time notification if collaborator was removed
        if (collaborator != null) {
            notifyPermissionChange(mindmapId, "collaborator_removed", Map.of(
                    "email", email,
                    "userId", collaborator.getMysqlUserId() != null ? collaborator.getMysqlUserId() : 0));
        }
    }

    /**
     * Chấp nhận lời mời cộng tác viên
     */
    @Transactional
    public void acceptInvitation(String token, Long userId) {
        log.info("User {} accepting invitation with token: {}", userId, token);

        CollaborationInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Lời mời không tồn tại hoặc đã hết hạn."));

        if ("accepted".equals(invitation.getStatus())) {
            throw new IllegalArgumentException("Lời mời đã được chấp nhận trước đó.");
        }

        if ("rejected".equals(invitation.getStatus())) {
            throw new IllegalArgumentException("Lời mời đã bị từ chối.");
        }

        if (LocalDateTime.now().isAfter(invitation.getExpiresAt())) {
            throw new IllegalArgumentException("Lời mời đã hết hạn.");
        }

        Mindmap mindmap = mindmapRepository.findById(invitation.getMindmapId())
                .orElseThrow(() -> new MindmapNotFoundException(invitation.getMindmapId(), userId));

        // Find or create collaborator
        Collaborator collaborator = mindmap.getCollaborators().stream()
                .filter(c -> c.getMysqlUserId() != null && c.getMysqlUserId().equals(userId))
                .findFirst()
                .orElse(null);

        if (collaborator == null) {
            // Collaborator doesn't exist (user signed up after being invited)
            log.info("Creating new collaborator entry for user {} in mindmap {}", userId, mindmap.getId());
            User acceptingUser = userRepository.findById(userId).orElse(null);
            String userEmail = (acceptingUser != null) ? acceptingUser.getEmail() : invitation.getInvitedEmail();

            collaborator = Collaborator.builder()
                    .mysqlUserId(userId)
                    .email(userEmail)
                    .role(invitation.getRole().toString())
                    .invitedBy(invitation.getInvitedByUserId())
                    .invitedAt(invitation.getCreatedAt())
                    .status("pending")
                    .build();

            mindmap.getCollaborators().add(collaborator);
            log.info("Collaborator {} added to mindmap {} with pending status", userEmail, mindmap.getId());
        }

        log.info("Updating collaborator status for user {} in mindmap {} to accepted", userId, mindmap.getId());
        collaborator.setStatus("accepted");
        collaborator.setAcceptedAt(LocalDateTime.now());

        Mindmap updatedMindmap = mindmapRepository.save(mindmap);
        log.info("Mindmap saved after accepting invitation. Updated collaborator status in mindmap {}",
                updatedMindmap.getId());

        // Cập nhật status của lời mời
        invitation.setStatus("accepted");
        invitation.setAcceptedAt(LocalDateTime.now());
        invitation.setAcceptedByUserId(userId);
        invitationRepository.save(invitation);

        log.info("Invitation {} accepted by user {}", token, userId);
    }

    /**
     * Từ chối lời mời cộng tác viên
     */
    @Transactional
    public void rejectInvitation(String token, Long userId) {
        log.info("User {} rejecting invitation with token: {}", userId, token);

        CollaborationInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Lời mời không tồn tại hoặc đã hết hạn."));

        if (!"pending".equals(invitation.getStatus())) {
            throw new IllegalArgumentException("Chỉ có thể từ chối lời mời đang chờ xác nhận.");
        }

        invitation.setStatus("rejected");
        invitation.setRejectedAt(LocalDateTime.now());
        invitation.setRejectedByUserId(userId);
        invitationRepository.save(invitation);

        // Xóa collaborator khỏi mindmap nếu status là pending
        if (invitation.getInvitedUserId() != null) {
            Mindmap mindmap = mindmapRepository.findById(invitation.getMindmapId())
                    .orElse(null);
            if (mindmap != null) {
                mindmap.getCollaborators().removeIf(c -> c.getMysqlUserId() != null && c.getMysqlUserId().equals(userId)
                        && "pending".equals(c.getStatus()));
                mindmapRepository.save(mindmap);
            }
        }

        log.info("Invitation {} rejected by user {}", token, userId);
    }

    /**
     * Lấy lời mời bằng token
     */
    public CollaborationInvitation getInvitationByToken(String token) {
        log.info("Getting invitation with token: {}", token);
        return invitationRepository.findByToken(token).orElse(null);
    }

    /**
     * Lấy danh sách lời mời đang chờ xác nhận cho một mindmap
     */
    public List<CollaborationInvitation> getPendingInvitations(String mindmapId, Long userId) {
        log.info("Getting pending invitations for mindmap: {} by user: {}", mindmapId, userId);

        Mindmap mindmap = mindmapRepository.findById(mindmapId)
                .orElseThrow(() -> new MindmapNotFoundException(mindmapId, userId));

        // Chỉ chủ sở hữu mới có quyền xem lời mời
        if (!mindmap.getMysqlUserId().equals(userId)) {
            throw new MindmapAccessDeniedException("Chỉ chủ sở hữu mới có quyền xem lời mời.", mindmapId, userId);
        }

        // Return all pending invitations (including those for unregistered users)
        // This will show all pending invites regardless of whether user has registered
        // or been added as collaborator
        return invitationRepository.findByMindmapIdAndStatus(mindmapId, "pending");
    }

    /**
     * Collaborator leaves the mindmap
     */
    @Transactional
    public void leaveCollaboration(String mindmapId, Long userId) {
        log.info("User {} leaving collaboration on mindmap {}", userId, mindmapId);

        Mindmap mindmap = mindmapRepository.findById(mindmapId)
                .orElseThrow(() -> new MindmapNotFoundException(mindmapId, userId));

        // Find and remove the collaborator
        boolean removed = mindmap.getCollaborators()
                .removeIf(c -> c.getMysqlUserId() != null && c.getMysqlUserId().equals(userId));

        if (!removed) {
            throw new IllegalArgumentException("Người dùng này không phải là collaborator của mindmap này.");
        }

        mindmapRepository.save(mindmap);
        log.info("User {} successfully left mindmap {}", userId, mindmapId);
    }

    /**
     * Send notification to real-time server about permission changes
     */
    private void notifyPermissionChange(String mindmapId, String eventType, Map<String, Object> data) {
        try {
            String url = realtimeServerUrl + "/notify/permission-change";

            Map<String, Object> payload = new HashMap<>();
            payload.put("mindmapId", mindmapId);
            payload.put("eventType", eventType);
            payload.put("data", data);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            restTemplate.postForEntity(url, request, String.class);
            log.info("Sent permission change notification: {} for mindmap: {}", eventType, mindmapId);
        } catch (Exception e) {
            log.error("Failed to send permission change notification: {}", e.getMessage());
            // Don't throw exception, notification failure shouldn't break the operation
        }
    }
}