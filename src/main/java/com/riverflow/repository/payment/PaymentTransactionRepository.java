package com.riverflow.repository.payment;

import com.riverflow.model.payment.PaymentTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long>,
                JpaSpecificationExecutor<PaymentTransaction> {

        /**
         * Tìm tất cả giao dịch của một User, có phân trang và sắp xếp.
         * Spring Data JPA sẽ tự động tạo query dựa trên tên phương thức.
         * * @param userId ID của user
         * 
         * @param pageable Thông tin phân trang (page, size, sort)
         * @return Trang dữ liệu PaymentTransaction
         */
        Page<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

        /**
         * Count transactions by status
         */
        @Query("SELECT COUNT(p) FROM PaymentTransaction p WHERE p.status = :status")
        Long countByStatus(@Param("status") PaymentTransaction.TransactionStatus status);

        /**
         * Sum transfer amounts by status
         */
        @Query("SELECT COALESCE(SUM(p.transferAmount), 0) FROM PaymentTransaction p WHERE p.status = :status")
        Long sumAmountByStatus(@Param("status") PaymentTransaction.TransactionStatus status);

        /**
         * Count transactions after a specific date
         */
        @Query("SELECT COUNT(p) FROM PaymentTransaction p WHERE p.createdAt >= :startDate")
        Long countByCreatedAtAfter(@Param("startDate") LocalDateTime startDate);

        /**
         * Sum amounts for transactions after a specific date
         */
        @Query("SELECT COALESCE(SUM(p.transferAmount), 0) FROM PaymentTransaction p WHERE p.createdAt >= :startDate")
        Long sumAmountByCreatedAtAfter(@Param("startDate") LocalDateTime startDate);

        /**
         * Total sum of all transfer amounts
         */
        @Query("SELECT COALESCE(SUM(p.transferAmount), 0) FROM PaymentTransaction p")
        Long sumTotalAmount();

        // ==================== REPORT QUERIES ====================

        /**
         * Sum amounts for processed transactions between dates
         */
        @Query(value = "SELECT COALESCE(SUM(transfer_amount), 0) FROM payment_transactions " +
                        "WHERE status = 'processed' AND transfer_type = 'in' " +
                        "AND created_at BETWEEN :startDate AND :endDate", nativeQuery = true)
        Long sumProcessedAmountBetween(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        /**
         * Count transactions between dates
         */
        @Query("SELECT COUNT(p) FROM PaymentTransaction p WHERE p.createdAt BETWEEN :startDate AND :endDate")
        Long countByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        /**
         * Get daily revenue (processed payments) for a date range
         * Returns list of Object[] where [0] = date, [1] = sum amount, [2] = count
         */
        @Query(value = "SELECT DATE(created_at) as date, " +
                        "COALESCE(SUM(transfer_amount), 0) as amount, " +
                        "COUNT(*) as count " +
                        "FROM payment_transactions " +
                        "WHERE status = 'processed' AND transfer_type = 'in' " +
                        "AND created_at BETWEEN :startDate AND :endDate " +
                        "GROUP BY DATE(created_at) " +
                        "ORDER BY date ASC", nativeQuery = true)
        java.util.List<Object[]> getDailyRevenue(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        /**
         * Get weekly revenue for a date range
         * Returns list of Object[] where [0] = year, [1] = week, [2] = sum amount, [3]
         * = count
         */
        @Query(value = "SELECT YEAR(created_at) as year, WEEK(created_at) as week, " +
                        "COALESCE(SUM(transfer_amount), 0) as amount, " +
                        "COUNT(*) as count " +
                        "FROM payment_transactions " +
                        "WHERE status = 'processed' AND transfer_type = 'in' " +
                        "AND created_at BETWEEN :startDate AND :endDate " +
                        "GROUP BY YEAR(created_at), WEEK(created_at) " +
                        "ORDER BY year ASC, week ASC", nativeQuery = true)
        java.util.List<Object[]> getWeeklyRevenue(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        /**
         * Get monthly revenue for a date range
         * Returns list of Object[] where [0] = year, [1] = month, [2] = sum amount, [3]
         * = count
         */
        @Query(value = "SELECT YEAR(created_at) as year, MONTH(created_at) as month, " +
                        "COALESCE(SUM(transfer_amount), 0) as amount, " +
                        "COUNT(*) as count " +
                        "FROM payment_transactions " +
                        "WHERE status = 'processed' AND transfer_type = 'in' " +
                        "AND created_at BETWEEN :startDate AND :endDate " +
                        "GROUP BY YEAR(created_at), MONTH(created_at) " +
                        "ORDER BY year ASC, month ASC", nativeQuery = true)
        java.util.List<Object[]> getMonthlyRevenue(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        /**
         * Get total processed revenue (in transfers only)
         */
        @Query(value = "SELECT COALESCE(SUM(transfer_amount), 0) FROM payment_transactions " +
                        "WHERE status = 'processed' AND transfer_type = 'in'", nativeQuery = true)
        Long sumTotalProcessedRevenue();

        /**
         * Get processed revenue after a specific date
         */
        @Query(value = "SELECT COALESCE(SUM(transfer_amount), 0) FROM payment_transactions " +
                        "WHERE status = 'processed' AND transfer_type = 'in' " +
                        "AND created_at >= :startDate", nativeQuery = true)
        Long sumProcessedRevenueAfter(@Param("startDate") LocalDateTime startDate);
}