package com.riverflow.service;

import com.riverflow.dto.support.*;
import com.riverflow.model.User;
import com.riverflow.model.support.SupportTicket;
import com.riverflow.model.support.SupportTicketAttachment;
import com.riverflow.model.support.SupportTicketMessage;
import com.riverflow.repository.SupportTicketAttachmentRepository;
import com.riverflow.repository.SupportTicketMessageRepository;
import com.riverflow.repository.SupportTicketRepository;
import com.riverflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for user-facing support ticket operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final SupportTicketMessageRepository messageRepository;
    private final SupportTicketAttachmentRepository attachmentRepository;
    private final UserRepository userRepository;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int MAX_ATTACHMENTS_PER_MESSAGE = 5;

    /**
     * Create a new support ticket
     */
    @Transactional
    public SupportTicketResponse createTicket(Long userId, SupportTicketRequest request,
            List<MultipartFile> attachments) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Generate unique ticket number
        String ticketNumber = generateTicketNumber();

        // Create ticket
        SupportTicket ticket = SupportTicket.builder()
                .user(user)
                .ticketNumber(ticketNumber)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory() != null ? request.getCategory() : SupportTicket.Category.OTHER)
                .priority(request.getPriority() != null ? request.getPriority() : SupportTicket.Priority.MEDIUM)
                .status(SupportTicket.Status.NEW)
                .build();

        ticket = ticketRepository.save(ticket);

        // Create initial message with description
        SupportTicketMessage initialMessage = SupportTicketMessage.builder()
                .ticket(ticket)
                .sender(user)
                .message(request.getDescription())
                .isInternalNote(false)
                .build();

        initialMessage = messageRepository.save(initialMessage);

        // Handle attachments
        if (attachments != null && !attachments.isEmpty()) {
            saveAttachments(initialMessage, attachments);
        }

        log.info("Created support ticket {} for user {}", ticketNumber, userId);

        return mapToResponse(ticket, false);
    }

    /**
     * Get user's tickets with pagination
     */
    @Transactional(readOnly = true)
    public Page<SupportTicketResponse> getMyTickets(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SupportTicket> tickets = ticketRepository.findByUserId(userId, pageable);
        return tickets.map(ticket -> mapToResponse(ticket, false));
    }

    /**
     * Get a specific ticket by ID (for user)
     */
    @Transactional(readOnly = true)
    public SupportTicketResponse getTicketById(Long userId, Long ticketId) {
        SupportTicket ticket = ticketRepository.findByIdAndUserId(ticketId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        return mapToResponse(ticket, false);
    }

    /**
     * Add a reply to a ticket
     */
    @Transactional
    public SupportTicketMessageResponse replyToTicket(Long userId, Long ticketId,
            SupportTicketMessageRequest request, List<MultipartFile> attachments) {

        SupportTicket ticket = ticketRepository.findByIdAndUserId(ticketId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        // Check if ticket is not closed
        if (ticket.getStatus() == SupportTicket.Status.CLOSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot reply to a closed ticket");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Create message (users cannot create internal notes)
        SupportTicketMessage message = SupportTicketMessage.builder()
                .ticket(ticket)
                .sender(user)
                .message(request.getMessage())
                .isInternalNote(false) // Users cannot create internal notes
                .build();

        message = messageRepository.save(message);

        // Handle attachments
        if (attachments != null && !attachments.isEmpty()) {
            saveAttachments(message, attachments);
        }

        // Update ticket status to waiting for response (admin needs to respond)
        if (ticket.getStatus() == SupportTicket.Status.WAITING_FOR_RESPONSE) {
            ticket.setStatus(SupportTicket.Status.IN_PROGRESS);
            ticketRepository.save(ticket);
        }

        log.info("User {} replied to ticket {}", userId, ticket.getTicketNumber());

        return mapToMessageResponse(message);
    }

    /**
     * Generate unique ticket number in format: TKT-YYYYMMDD-XXX
     */
    private String generateTicketNumber() {
        String datePrefix = "TKT-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";

        return ticketRepository.findMaxTicketNumberByPrefix(datePrefix)
                .map(maxNumber -> {
                    String sequencePart = maxNumber.substring(datePrefix.length());
                    int nextSequence = Integer.parseInt(sequencePart) + 1;
                    return datePrefix + String.format("%03d", nextSequence);
                })
                .orElse(datePrefix + "001");
    }

    /**
     * Save attachments to a message
     */
    private void saveAttachments(SupportTicketMessage message, List<MultipartFile> files) {
        if (files.size() > MAX_ATTACHMENTS_PER_MESSAGE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Maximum " + MAX_ATTACHMENTS_PER_MESSAGE + " attachments allowed per message");
        }

        for (MultipartFile file : files) {
            if (file.isEmpty())
                continue;

            if (file.getSize() > MAX_FILE_SIZE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "File " + file.getOriginalFilename() + " exceeds maximum size of 10MB");
            }

            try {
                SupportTicketAttachment attachment = SupportTicketAttachment.builder()
                        .message(message)
                        .fileName(file.getOriginalFilename())
                        .mimeType(file.getContentType())
                        .fileData(file.getBytes())
                        .fileSize(file.getSize())
                        .build();

                attachmentRepository.save(attachment);
            } catch (IOException e) {
                log.error("Failed to save attachment: {}", file.getOriginalFilename(), e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save attachment");
            }
        }
    }

    /**
     * Map SupportTicket to SupportTicketResponse
     */
    public SupportTicketResponse mapToResponse(SupportTicket ticket, boolean includeInternalNotes) {
        List<SupportTicketMessage> messages = includeInternalNotes
                ? messageRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId())
                : messageRepository.findByTicketIdAndIsInternalNoteFalseOrderByCreatedAtAsc(ticket.getId());

        List<SupportTicketMessageResponse> messageResponses = messages.stream()
                .map(this::mapToMessageResponse)
                .collect(Collectors.toList());

        return SupportTicketResponse.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .category(ticket.getCategory())
                .priority(ticket.getPriority())
                .status(ticket.getStatus())
                .userId(ticket.getUser().getId())
                .userEmail(ticket.getUser().getEmail())
                .userFullName(ticket.getUser().getFullName())
                .assignedToId(ticket.getAssignedTo() != null ? ticket.getAssignedTo().getId() : null)
                .assignedToEmail(ticket.getAssignedTo() != null ? ticket.getAssignedTo().getEmail() : null)
                .assignedToFullName(ticket.getAssignedTo() != null ? ticket.getAssignedTo().getFullName() : null)
                .messages(messageResponses)
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .resolvedAt(ticket.getResolvedAt())
                .closedAt(ticket.getClosedAt())
                .build();
    }

    /**
     * Map SupportTicketMessage to SupportTicketMessageResponse
     */
    public SupportTicketMessageResponse mapToMessageResponse(SupportTicketMessage message) {
        List<SupportTicketAttachment> attachments = attachmentRepository.findByMessageId(message.getId());

        List<SupportTicketAttachmentResponse> attachmentResponses = attachments.stream()
                .map(this::mapToAttachmentResponse)
                .collect(Collectors.toList());

        User sender = message.getSender();
        String avatarUrl = sender.getId() != null
                ? "/api/users/" + sender.getId() + "/avatar"
                : null;

        return SupportTicketMessageResponse.builder()
                .id(message.getId())
                .message(message.getMessage())
                .isInternalNote(message.getIsInternalNote())
                .senderId(sender.getId())
                .senderEmail(sender.getEmail())
                .senderFullName(sender.getFullName())
                .senderRole(sender.getRole().name().toUpperCase())
                .senderAvatarUrl(avatarUrl)
                .attachments(attachmentResponses)
                .createdAt(message.getCreatedAt())
                .build();
    }

    /**
     * Map SupportTicketAttachment to SupportTicketAttachmentResponse
     */
    private SupportTicketAttachmentResponse mapToAttachmentResponse(SupportTicketAttachment attachment) {
        return SupportTicketAttachmentResponse.builder()
                .id(attachment.getId())
                .fileName(attachment.getFileName())
                .mimeType(attachment.getMimeType())
                .fileSize(attachment.getFileSize())
                .downloadUrl("/api/support-tickets/attachments/" + attachment.getId())
                .createdAt(attachment.getCreatedAt())
                .build();
    }

    /**
     * Get attachment by ID (for download)
     */
    @Transactional(readOnly = true)
    public SupportTicketAttachment getAttachment(Long attachmentId, Long userId) {
        SupportTicketAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"));

        // Verify user has access to this attachment
        SupportTicketMessage message = attachment.getMessage();
        SupportTicket ticket = message.getTicket();

        if (!ticket.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return attachment;
    }
}
