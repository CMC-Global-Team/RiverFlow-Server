package com.riverflow.controller;

import com.riverflow.config.jwt.UserPrincipal;
import com.riverflow.dto.support.SupportTicketMessageRequest;
import com.riverflow.dto.support.SupportTicketMessageResponse;
import com.riverflow.dto.support.SupportTicketRequest;
import com.riverflow.dto.support.SupportTicketResponse;
import com.riverflow.model.support.SupportTicketAttachment;
import com.riverflow.service.SupportTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST Controller for user support ticket operations
 * All endpoints require authenticated user
 */
@RestController
@RequestMapping("/support-tickets")
@RequiredArgsConstructor
public class SupportTicketController {

    private final SupportTicketService supportTicketService;

    /**
     * Create a new support ticket
     * POST /api/support-tickets
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SupportTicketResponse> createTicket(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestPart("ticket") SupportTicketRequest request,
            @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments) {

        SupportTicketResponse ticket = supportTicketService.createTicket(
                currentUser.getId(), request, attachments);
        return ResponseEntity.ok(ticket);
    }

    /**
     * Get user's tickets with pagination
     * GET /api/support-tickets?page=0&size=10
     */
    @GetMapping
    public ResponseEntity<Page<SupportTicketResponse>> getMyTickets(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<SupportTicketResponse> tickets = supportTicketService.getMyTickets(
                currentUser.getId(), page, size);
        return ResponseEntity.ok(tickets);
    }

    /**
     * Get a specific ticket by ID
     * GET /api/support-tickets/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<SupportTicketResponse> getTicketById(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id) {

        SupportTicketResponse ticket = supportTicketService.getTicketById(
                currentUser.getId(), id);
        return ResponseEntity.ok(ticket);
    }

    /**
     * Reply to a ticket
     * POST /api/support-tickets/{id}/messages
     */
    @PostMapping(value = "/{id}/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SupportTicketMessageResponse> replyToTicket(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id,
            @Valid @RequestPart("message") SupportTicketMessageRequest request,
            @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments) {

        SupportTicketMessageResponse response = supportTicketService.replyToTicket(
                currentUser.getId(), id, request, attachments);
        return ResponseEntity.ok(response);
    }

    /**
     * Download an attachment
     * GET /api/support-tickets/attachments/{attachmentId}
     */
    @GetMapping("/attachments/{attachmentId}")
    public ResponseEntity<Resource> downloadAttachment(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long attachmentId) {

        SupportTicketAttachment attachment = supportTicketService.getAttachment(
                attachmentId, currentUser.getId());

        ByteArrayResource resource = new ByteArrayResource(attachment.getFileData());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + attachment.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(attachment.getMimeType()))
                .contentLength(attachment.getFileSize())
                .body(resource);
    }
}
