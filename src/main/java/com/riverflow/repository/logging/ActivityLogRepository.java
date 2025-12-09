package com.riverflow.repository.logging;

import com.riverflow.model.logging.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MongoDB repository for ActivityLog documents.
 */
@Repository
public interface ActivityLogRepository extends MongoRepository<ActivityLog, String> {

        /**
         * Find logs by category
         */
        Page<ActivityLog> findByCategory(String category, Pageable pageable);

        /**
         * Find logs by action
         */
        Page<ActivityLog> findByAction(String action, Pageable pageable);

        /**
         * Find logs by actor role
         */
        Page<ActivityLog> findByActorRole(String actorRole, Pageable pageable);

        /**
         * Find logs within a date range
         */
        Page<ActivityLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

        /**
         * Find logs by actor ID
         */
        Page<ActivityLog> findByActorId(Long actorId, Pageable pageable);

        /**
         * Find logs by target ID
         */
        Page<ActivityLog> findByTargetId(String targetId, Pageable pageable);

        /**
         * Count logs by category
         */
        long countByCategory(String category);

        /**
         * Count logs by action
         */
        long countByAction(String action);

        /**
         * Count logs by actor role
         */
        long countByActorRole(String actorRole);

        /**
         * Count logs within a date range
         */
        long countByTimestampBetween(LocalDateTime start, LocalDateTime end);

        /**
         * Search logs with multiple filters
         */
        @Query("{ $and: [ " +
                        "{ $or: [ { 'actorEmail': { $regex: ?0, $options: 'i' } }, { 'details': { $regex: ?0, $options: 'i' } } ] }, "
                        +
                        "{ $or: [ { $expr: { $eq: [?1, null] } }, { 'category': ?1 } ] }, " +
                        "{ $or: [ { $expr: { $eq: [?2, null] } }, { 'action': ?2 } ] }, " +
                        "{ $or: [ { $expr: { $eq: [?3, null] } }, { 'actorRole': ?3 } ] }, " +
                        "{ $or: [ { $expr: { $eq: [?4, null] } }, { 'timestamp': { $gte: ?4 } } ] }, " +
                        "{ $or: [ { $expr: { $eq: [?5, null] } }, { 'timestamp': { $lte: ?5 } } ] } " +
                        "] }")
        Page<ActivityLog> searchLogs(
                        String search,
                        String category,
                        String action,
                        String actorRole,
                        LocalDateTime startDate,
                        LocalDateTime endDate,
                        Pageable pageable);

        /**
         * Get recent logs (for statistics)
         */
        List<ActivityLog> findTop100ByOrderByTimestampDesc();

        /**
         * Count logs by category in date range
         */
        long countByCategoryAndTimestampBetween(String category, LocalDateTime start, LocalDateTime end);
}
