package com.riverflow.repository;

import com.riverflow.model.support.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for SupportTicket entity
 */
@Repository
public interface SupportTicketRepository
        extends JpaRepository<SupportTicket, Long>, JpaSpecificationExecutor<SupportTicket> {

    /**
     * Find all tickets by user ID
     */
    Page<SupportTicket> findByUserId(Long userId, Pageable pageable);

    /**
     * Find a ticket by ID and user ID (for user access control)
     */
    Optional<SupportTicket> findByIdAndUserId(Long id, Long userId);

    /**
     * Find ticket by ticket number
     */
    Optional<SupportTicket> findByTicketNumber(String ticketNumber);

    /**
     * Count tickets by status
     */
    long countByStatus(SupportTicket.Status status);

    /**
     * Count tickets by priority
     */
    long countByPriority(SupportTicket.Priority priority);

    /**
     * Count tickets by category
     */
    long countByCategory(SupportTicket.Category category);

    /**
     * Count unassigned tickets
     */
    long countByAssignedToIsNull();

    /**
     * Count tickets assigned to a specific user
     */
    long countByAssignedToId(Long assignedToId);

    /**
     * Find the latest ticket number for today (for generating new ticket numbers)
     */
    @Query("SELECT MAX(t.ticketNumber) FROM SupportTicket t WHERE t.ticketNumber LIKE :prefix%")
    Optional<String> findMaxTicketNumberByPrefix(@Param("prefix") String prefix);
}
