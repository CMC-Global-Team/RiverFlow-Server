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
}