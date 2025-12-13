package com.riverflow.repository;

import com.riverflow.model.support.SupportTicketAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for SupportTicketAttachment entity
 */
@Repository
public interface SupportTicketAttachmentRepository extends JpaRepository<SupportTicketAttachment, Long> {

    /**
     * Find all attachments for a message
     */
    List<SupportTicketAttachment> findByMessageId(Long messageId);

    /**
     * Count attachments for a message
     */
    long countByMessageId(Long messageId);
}
