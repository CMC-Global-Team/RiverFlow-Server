package com.riverflow.repository.payment;

import com.riverflow.model.payment.CreditTopupRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CreditTopupRequestRepository extends JpaRepository<CreditTopupRequest, Long> {
    Optional<CreditTopupRequest> findByCode(String code);
}
