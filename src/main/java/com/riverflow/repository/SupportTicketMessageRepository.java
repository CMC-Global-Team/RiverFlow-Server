package com.riverflow.repository;

import com.riverflow.model.support.SupportTicketMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for SupportTicketMessage entity
 */
@Repository
public interface SupportTicketMessageRepository extends JpaRepository<SupportTicketMessage, Long> {

    /**
     * Find all messages for a ticket (ordered by creation time)
     */
    List<SupportTicketMessage> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

    /**
     * Find all non-internal messages for a ticket (for users)
     */
    List<SupportTicketMessage> findByTicketIdAndIsInternalNoteFalseOrderByCreatedAtAsc(Long ticketId);

    /**
     * Find messages with pagination
     */
    Page<SupportTicketMessage> findByTicketId(Long ticketId, Pageable pageable);

    /**
     * Count messages for a ticket
     */
    long countByTicketId(Long ticketId);
}
