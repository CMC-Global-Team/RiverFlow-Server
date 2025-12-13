package com.riverflow.repository.admin;

import com.riverflow.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for admin reporting queries on User entity
 */
@Repository
public interface AdminReportRepository extends JpaRepository<User, Long> {

    // ==================== COUNT QUERIES ====================

    /**
     * Count users by status
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = :status")
    Long countByStatus(@Param("status") User.UserStatus status);

    /**
     * Count users by role
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    Long countByRole(@Param("role") User.Role role);

    /**
     * Count users created after a specific date
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startDate")
    Long countByCreatedAtAfter(@Param("startDate") LocalDateTime startDate);

    /**
     * Count users created between dates
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    Long countByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Count active users (logged in within period)
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.lastLoginAt >= :since AND u.status = 'active'")
    Long countActiveUsersSince(@Param("since") LocalDateTime since);

    /**
     * Total count of all users
     */
    @Query("SELECT COUNT(u) FROM User u")
    Long countTotalUsers();

    // ==================== TIME SERIES QUERIES ====================

    /**
     * Get daily user registration counts for a date range
     * Returns list of Object[] where [0] = date, [1] = count
     */
    @Query(value = "SELECT DATE(created_at) as date, COUNT(*) as count " +
            "FROM users " +
            "WHERE created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY DATE(created_at) " +
            "ORDER BY date ASC", nativeQuery = true)
    List<Object[]> getDailyRegistrations(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Get weekly user registration counts
     * Returns list of Object[] where [0] = year, [1] = week, [2] = count
     */
    @Query(value = "SELECT YEAR(created_at) as year, WEEK(created_at) as week, COUNT(*) as count " +
            "FROM users " +
            "WHERE created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY YEAR(created_at), WEEK(created_at) " +
            "ORDER BY year ASC, week ASC", nativeQuery = true)
    List<Object[]> getWeeklyRegistrations(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Get monthly user registration counts
     * Returns list of Object[] where [0] = year, [1] = month, [2] = count
     */
    @Query(value = "SELECT YEAR(created_at) as year, MONTH(created_at) as month, COUNT(*) as count " +
            "FROM users " +
            "WHERE created_at BETWEEN :startDate AND :endDate " +
            "GROUP BY YEAR(created_at), MONTH(created_at) " +
            "ORDER BY year ASC, month ASC", nativeQuery = true)
    List<Object[]> getMonthlyRegistrations(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
