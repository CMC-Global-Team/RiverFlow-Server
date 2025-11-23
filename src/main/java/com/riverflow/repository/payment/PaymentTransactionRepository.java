package com.riverflow.repository.payment;

import com.riverflow.model.payment.PaymentTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    /**
     * Tìm tất cả giao dịch của một User, có phân trang và sắp xếp.
     * Spring Data JPA sẽ tự động tạo query dựa trên tên phương thức.
     * * @param userId ID của user
     * @param pageable Thông tin phân trang (page, size, sort)
     * @return Trang dữ liệu PaymentTransaction
     */
    Page<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

}