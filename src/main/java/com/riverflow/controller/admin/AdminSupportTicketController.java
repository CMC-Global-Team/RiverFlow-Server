package com.riverflow.controller.admin;

import com.riverflow.config.jwt.UserPrincipal;
import com.riverflow.dto.support.*;
import com.riverflow.model.support.SupportTicketAttachment;
import com.riverflow.service.admin.AdminSupportTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for admin support ticket management
 * All endpoints require ADMIN or SUPER_ADMIN role
 */
@RestController
@RequestMapping("/admin/support-tickets")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminSupportTicketController {

    private final AdminSupportTicketService adminSupportTicketService;

    /**
     * Get all tickets with filtering, sorting, and pagination
     * GET
     * /api/admin/support-tickets?page=0&size=10&status=NEW&priority=HIGH&category=BUG&assignedToId=1&sortBy=createdAt&sortDir=desc&search=keyword
     */
    @GetMapping
    public ResponseEntity<Page<SupportTicketResponse>> getAllTickets(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long assignedToId,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<SupportTicketResponse> tickets = adminSupportTicketService.getAllTickets(
                search, status, priority, category, assignedToId, sortBy, sortDir, page, size);
        return ResponseEntity.ok(tickets);
    }

    /**
     * Get ticket by ID (includes internal notes)
     * GET /api/admin/support-tickets/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<SupportTicketResponse> getTicketById(@PathVariable Long id) {
        SupportTicketResponse ticket = adminSupportTicketService.getTicketById(id);
        return ResponseEntity.ok(ticket);
    }

    /**
     * Update ticket (status, priority, assignment)
     * PUT /api/admin/support-tickets/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<SupportTicketResponse> updateTicket(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id,
            @Valid @RequestBody AdminSupportTicketUpdateRequest request) {

        SupportTicketResponse ticket = adminSupportTicketService.updateTicket(
                id, request, currentUser.getId());
        return ResponseEntity.ok(ticket);
    }

    /**
     * Reply to ticket (with optional internal note)
     * POST /api/admin/support-tickets/{id}/messages
     */
    @PostMapping(value = "/{id}/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SupportTicketMessageResponse> replyToTicket(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id,
            @Valid @RequestPart("message") SupportTicketMessageRequest request,
            @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments) {

        SupportTicketMessageResponse response = adminSupportTicketService.replyToTicket(
                id, currentUser.getId(), request, attachments);
        return ResponseEntity.ok(response);
    }

    /**
     * Close a ticket
     * PUT /api/admin/support-tickets/{id}/close
     */
    @PutMapping("/{id}/close")
    public ResponseEntity<SupportTicketResponse> closeTicket(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id) {

        SupportTicketResponse ticket = adminSupportTicketService.closeTicket(id, currentUser.getId());
        return ResponseEntity.ok(ticket);
    }

    /**
     * Get ticket statistics for dashboard
     * GET /api/admin/support-tickets/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<SupportTicketStatisticsResponse> getStatistics() {
        SupportTicketStatisticsResponse stats = adminSupportTicketService.getStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Download an attachment
     * GET /api/admin/support-tickets/attachments/{attachmentId}
     */
    @GetMapping("/attachments/{attachmentId}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long attachmentId) {
        SupportTicketAttachment attachment = adminSupportTicketService.getAttachment(attachmentId);

        ByteArrayResource resource = new ByteArrayResource(attachment.getFileData());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + attachment.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(attachment.getMimeType()))
                .contentLength(attachment.getFileSize())
                .body(resource);
    }
}
