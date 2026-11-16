package com.payflow.transaction.service;

import com.payflow.security.CustomUserDetails;
import com.payflow.transaction.dto.TransactionPageResponse;
import com.payflow.transaction.dto.TransactionResponse;
import com.payflow.transaction.entity.Transaction;
import com.payflow.transaction.entity.TransactionStatus;
import com.payflow.transaction.entity.TransactionType;
import com.payflow.transaction.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(
            TransactionRepository transactionRepository
    ) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public TransactionPageResponse getMyTransactions(
            int page,
            int size,
            TransactionType type,
            TransactionStatus status
    ) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        Long authenticatedUserId = userDetails.getId();

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"
                )
        );

        Page<Transaction> transactionPage;

        if (type != null && status != null) {

            transactionPage =
                    transactionRepository
                            .findByPaymentUserIdAndTypeAndStatus(
                                    authenticatedUserId,
                                    type,
                                    status,
                                    pageable
                            );

        } else if (type != null) {

            transactionPage =
                    transactionRepository
                            .findByPaymentUserIdAndType(
                                    authenticatedUserId,
                                    type,
                                    pageable
                            );

        } else if (status != null) {

            transactionPage =
                    transactionRepository
                            .findByPaymentUserIdAndStatus(
                                    authenticatedUserId,
                                    status,
                                    pageable
                            );

        } else {

            transactionPage =
                    transactionRepository
                            .findByPaymentUserId(
                                    authenticatedUserId,
                                    pageable
                            );
        }

        var content = transactionPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return new TransactionPageResponse(
                content,
                transactionPage.getNumber(),
                transactionPage.getSize(),
                transactionPage.getTotalElements(),
                transactionPage.getTotalPages()
        );
    }

    private TransactionResponse toResponse(
            Transaction transaction
    ) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getPayment().getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getStatus(),
                transaction.getCreatedAt()
        );
    }
}