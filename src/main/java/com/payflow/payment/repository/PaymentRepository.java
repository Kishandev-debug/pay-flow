package com.payflow.payment.repository;

import com.payflow.payment.entity.Payment;
import com.payflow.payment.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findByUserIdAndIdempotencyKey(
            Long userId,
            String idempotencyKey
    );

    Page<Payment> findByUserId(
            Long userId,
            Pageable pageable
    );

    Page<Payment> findByUserIdAndStatus(
            Long userId,
            PaymentStatus status,
            Pageable pageable
    );
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :paymentId")
    Optional<Payment> findByIdForUpdate(
            @Param("paymentId") Long paymentId
    );
}