package com.riverflow.service.admin;

import com.riverflow.dto.support.*;
import com.riverflow.model.User;
import com.riverflow.model.support.SupportTicket;
import com.riverflow.model.support.SupportTicketAttachment;
import com.riverflow.model.support.SupportTicketMessage;
import com.riverflow.repository.SupportTicketAttachmentRepository;
import com.riverflow.repository.SupportTicketMessageRepository;
import com.riverflow.repository.SupportTicketRepository;
import com.riverflow.repository.UserRepository;
import com.riverflow.service.NotificationService;
import com.riverflow.service.SupportTicketService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for admin support ticket management operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminSupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final SupportTicketMessageRepository messageRepository;
    private final SupportTicketAttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final SupportTicketService supportTicketService;
    private final NotificationService notificationService;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int MAX_ATTACHMENTS_PER_MESSAGE = 5;

    /**
     * Get all tickets with filtering, sorting, and pagination
     */
    @Transactional(readOnly = true)
    public Page<SupportTicketResponse> getAllTickets(
            String search,
            String status,
            String priority,
            String category,
            Long assignedToId,
            String sortBy,
            String sortDir,
            int page,
            int size) {

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<SupportTicket> spec = buildSpecification(search, status, priority, category, assignedToId);
        Page<SupportTicket> tickets = ticketRepository.findAll(spec, pageable);

        return tickets.map(ticket -> supportTicketService.mapToResponse(ticket, true));
    }

    /**
     * Build specification for filtering tickets
     */
    private Specification<SupportTicket> buildSpecification(
            String search, String status, String priority, String category, Long assignedToId) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search by ticket number, title, or user email
            if (search != null && !search.isBlank()) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                Predicate ticketNumberLike = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("ticketNumber")), searchPattern);
                Predicate titleLike = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("title")), searchPattern);
                Predicate userEmailLike = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("user").get("email")), searchPattern);
                predicates.add(criteriaBuilder.or(ticketNumberLike, titleLike, userEmailLike));
            }

            // Filter by status
            if (status != null && !status.isBlank()) {
                try {
                    SupportTicket.Status ticketStatus = SupportTicket.Status.valueOf(status.toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("status"), ticketStatus));
                } catch (IllegalArgumentException ignored) {
                    // Invalid status, ignore filter
                }
            }

            // Filter by priority
            if (priority != null && !priority.isBlank()) {
                try {
                    SupportTicket.Priority ticketPriority = SupportTicket.Priority.valueOf(priority.toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("priority"), ticketPriority));
                } catch (IllegalArgumentException ignored) {
                    // Invalid priority, ignore filter
                }
            }

            // Filter by category
            if (category != null && !category.isBlank()) {
                try {
                    SupportTicket.Category ticketCategory = SupportTicket.Category.valueOf(category.toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("category"), ticketCategory));
                } catch (IllegalArgumentException ignored) {
                    // Invalid category, ignore filter
                }
            }

            // Filter by assigned user
            if (assignedToId != null) {
                if (assignedToId == 0) {
                    // Unassigned tickets
                    predicates.add(criteriaBuilder.isNull(root.get("assignedTo")));
                } else {
                    predicates.add(criteriaBuilder.equal(root.get("assignedTo").get("id"), assignedToId));
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Get ticket by ID (admin view includes internal notes)
     */
    @Transactional(readOnly = true)
    public SupportTicketResponse getTicketById(Long ticketId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        return supportTicketService.mapToResponse(ticket, true);
    }

    /**
     * Update ticket (status, priority, assignment)
     */
    @Transactional
    public SupportTicketResponse updateTicket(Long ticketId, AdminSupportTicketUpdateRequest request, Long adminId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        boolean updated = false;

        // Update status
        if (request.getStatus() != null && request.getStatus() != ticket.getStatus()) {
            SupportTicket.Status oldStatus = ticket.getStatus();
            ticket.setStatus(request.getStatus());
            updated = true;

            // Set timestamps based on status changes
            if (request.getStatus() == SupportTicket.Status.RESOLVED && ticket.getResolvedAt() == null) {
                ticket.setResolvedAt(LocalDateTime.now());
            } else if (request.getStatus() == SupportTicket.Status.CLOSED && ticket.getClosedAt() == null) {
                ticket.setClosedAt(LocalDateTime.now());
                if (ticket.getResolvedAt() == null) {
                    ticket.setResolvedAt(LocalDateTime.now());
                }
            }

            log.info("Admin {} changed ticket {} status from {} to {}",
                    adminId, ticket.getTicketNumber(), oldStatus, request.getStatus());
        }

        // Update priority
        if (request.getPriority() != null && request.getPriority() != ticket.getPriority()) {
            SupportTicket.Priority oldPriority = ticket.getPriority();
            ticket.setPriority(request.getPriority());
            updated = true;
            log.info("Admin {} changed ticket {} priority from {} to {}",
                    adminId, ticket.getTicketNumber(), oldPriority, request.getPriority());
        }

        // Update assignment
        if (request.getAssignedToId() != null) {
            if (request.getAssignedToId() == 0) {
                // Unassign
                ticket.setAssignedTo(null);
                updated = true;
                log.info("Admin {} unassigned ticket {}", adminId, ticket.getTicketNumber());
            } else {
                User assignee = userRepository.findById(request.getAssignedToId())
                        .orElseThrow(
                                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignee user not found"));

                // Verify assignee is admin or super_admin
                if (assignee.getRole() != User.Role.admin && assignee.getRole() != User.Role.super_admin) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Ticket can only be assigned to admin users");
                }

                ticket.setAssignedTo(assignee);
                updated = true;
                log.info("Admin {} assigned ticket {} to {}",
                        adminId, ticket.getTicketNumber(), assignee.getEmail());
            }
        }

        if (updated) {
            ticket = ticketRepository.save(ticket);

            // Notify ticket owner about status update
            if (request.getStatus() != null) {
                notificationService.createNotification(
                        ticket.getUser().getId(),
                        NotificationService.TYPE_TICKET_UPDATE,
                        "Ticket Status Updated",
                        "Your ticket #" + ticket.getTicketNumber() + " status has been updated to "
                                + ticket.getStatus().name(),
                        "ticket",
                        ticket.getId().toString(),
                        "/dashboard/tickets/" + ticket.getId(),
                        "View Ticket");
            }
        }

        return supportTicketService.mapToResponse(ticket, true);
    }

    /**
     * Add admin reply to ticket (with support for internal notes)
     */
    @Transactional
    public SupportTicketMessageResponse replyToTicket(Long ticketId, Long adminId,
            SupportTicketMessageRequest request, List<MultipartFile> attachments) {

        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin user not found"));

        // Create message
        SupportTicketMessage message = SupportTicketMessage.builder()
                .ticket(ticket)
                .sender(admin)
                .message(request.getMessage())
                .isInternalNote(request.getIsInternalNote() != null ? request.getIsInternalNote() : false)
                .build();

        message = messageRepository.save(message);

        // Handle attachments
        if (attachments != null && !attachments.isEmpty()) {
            saveAttachments(message, attachments);
        }

        // If not internal note and ticket was not waiting, update status
        if (!message.getIsInternalNote() && ticket.getStatus() != SupportTicket.Status.WAITING_FOR_RESPONSE
                && ticket.getStatus() != SupportTicket.Status.CLOSED
                && ticket.getStatus() != SupportTicket.Status.RESOLVED) {
            ticket.setStatus(SupportTicket.Status.WAITING_FOR_RESPONSE);
            ticketRepository.save(ticket);
        }

        log.info("Admin {} {} to ticket {}", adminId,
                message.getIsInternalNote() ? "added internal note" : "replied",
                ticket.getTicketNumber());

        // Notify ticket owner about admin response (only for non-internal notes)
        if (!message.getIsInternalNote()) {
            notificationService.createNotification(
                    ticket.getUser().getId(),
                    NotificationService.TYPE_TICKET_RESPONSE,
                    "New Response on Ticket",
                    "Admin has responded to your ticket #" + ticket.getTicketNumber(),
                    "ticket",
                    ticket.getId().toString(),
                    "/dashboard/tickets/" + ticket.getId(),
                    "View Response");
        }

        return supportTicketService.mapToMessageResponse(message);
    }

    /**
     * Close a ticket
     */
    @Transactional
    public SupportTicketResponse closeTicket(Long ticketId, Long adminId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        if (ticket.getStatus() == SupportTicket.Status.CLOSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket is already closed");
        }

        ticket.setStatus(SupportTicket.Status.CLOSED);
        ticket.setClosedAt(LocalDateTime.now());
        if (ticket.getResolvedAt() == null) {
            ticket.setResolvedAt(LocalDateTime.now());
        }

        ticket = ticketRepository.save(ticket);
        log.info("Admin {} closed ticket {}", adminId, ticket.getTicketNumber());

        return supportTicketService.mapToResponse(ticket, true);
    }

    /**
     * Get ticket statistics for dashboard
     */
    @Transactional(readOnly = true)
    public SupportTicketStatisticsResponse getStatistics() {
        long totalTickets = ticketRepository.count();

        // Count by status
        Map<String, Long> ticketsByStatus = new HashMap<>();
        long openTickets = 0;
        long resolvedTickets = 0;
        long closedTickets = 0;

        for (SupportTicket.Status status : SupportTicket.Status.values()) {
            long count = ticketRepository.countByStatus(status);
            ticketsByStatus.put(status.name(), count);

            if (status == SupportTicket.Status.RESOLVED) {
                resolvedTickets = count;
            } else if (status == SupportTicket.Status.CLOSED) {
                closedTickets = count;
            } else {
                openTickets += count;
            }
        }

        // Count by priority
        Map<String, Long> ticketsByPriority = new HashMap<>();
        for (SupportTicket.Priority priority : SupportTicket.Priority.values()) {
            ticketsByPriority.put(priority.name(), ticketRepository.countByPriority(priority));
        }

        // Count by category
        Map<String, Long> ticketsByCategory = new HashMap<>();
        for (SupportTicket.Category category : SupportTicket.Category.values()) {
            ticketsByCategory.put(category.name(), ticketRepository.countByCategory(category));
        }

        // Count unassigned
        long unassignedTickets = ticketRepository.countByAssignedToIsNull();

        return SupportTicketStatisticsResponse.builder()
                .totalTickets(totalTickets)
                .openTickets(openTickets)
                .resolvedTickets(resolvedTickets)
                .closedTickets(closedTickets)
                .ticketsByStatus(ticketsByStatus)
                .ticketsByPriority(ticketsByPriority)
                .ticketsByCategory(ticketsByCategory)
                .unassignedTickets(unassignedTickets)
                .build();
    }

    /**
     * Get attachment by ID (for admin download)
     */
    @Transactional(readOnly = true)
    public SupportTicketAttachment getAttachment(Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"));
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
}
