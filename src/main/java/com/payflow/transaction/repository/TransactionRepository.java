package com.payflow.transaction.repository;

import com.payflow.transaction.entity.Transaction;
import com.payflow.transaction.entity.TransactionStatus;
import com.payflow.transaction.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository
        extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByPaymentUserId(
            Long userId,
            Pageable pageable
    );

    Page<Transaction> findByPaymentUserIdAndType(
            Long userId,
            TransactionType type,
            Pageable pageable
    );

    Page<Transaction> findByPaymentUserIdAndStatus(
            Long userId,
            TransactionStatus status,
            Pageable pageable
    );

    Page<Transaction> findByPaymentUserIdAndTypeAndStatus(
            Long userId,
            TransactionType type,
            TransactionStatus status,
            Pageable pageable
    );
}