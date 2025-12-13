package com.riverflow.model.support;

import com.riverflow.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a message in a support ticket thread
 */
@Entity
@Table(name = "support_ticket_messages", indexes = {
        @Index(name = "idx_ticket_id", columnList = "ticket_id"),
        @Index(name = "idx_sender_id", columnList = "sender_id"),
        @Index(name = "idx_created_at", columnList = "created_at"),
        @Index(name = "idx_internal_note", columnList = "is_internal_note")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicketMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private SupportTicket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_internal_note", nullable = false)
    @Builder.Default
    private Boolean isInternalNote = false;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SupportTicketAttachment> attachments = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
