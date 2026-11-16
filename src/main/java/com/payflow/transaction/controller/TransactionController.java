package com.payflow.transaction.controller;

import com.payflow.transaction.dto.TransactionPageResponse;
import com.payflow.transaction.entity.TransactionStatus;
import com.payflow.transaction.entity.TransactionType;
import com.payflow.transaction.service.TransactionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(
            TransactionService transactionService
    ) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public TransactionPageResponse getMyTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status
    ) {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page number cannot be negative"
            );
        }

        if (size < 1 || size > 50) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 50"
            );
        }

        TransactionType transactionType = null;

        if (type != null && !type.isBlank()) {
            try {
                transactionType =
                        TransactionType.valueOf(
                                type.trim().toUpperCase()
                        );
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "Invalid transaction type: " + type
                );
            }
        }

        TransactionStatus transactionStatus = null;

        if (status != null && !status.isBlank()) {
            try {
                transactionStatus =
                        TransactionStatus.valueOf(
                                status.trim().toUpperCase()
                        );
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "Invalid transaction status: " + status
                );
            }
        }

        return transactionService.getMyTransactions(
                page,
                size,
                transactionType,
                transactionStatus
        );
    }
}