package com.payflow.payment.controller;

import com.payflow.payment.dto.PaymentPageResponse;
import com.payflow.payment.dto.PaymentRequest;
import com.payflow.payment.dto.PaymentResponse;
import com.payflow.payment.entity.PaymentStatus;
import com.payflow.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public PaymentResponse createPayment(
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        return paymentService.createPayment(
                request,
                idempotencyKey
        );
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse getPaymentById(
            @PathVariable Long paymentId
    ) {
        return paymentService.getPaymentById(paymentId);
    }

        @GetMapping
        public PaymentPageResponse getMyPayments(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
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

            PaymentStatus paymentStatus = null;

            if (status != null && !status.isBlank()) {
                try {
                    paymentStatus = PaymentStatus.valueOf(
                            status.trim().toUpperCase()
                    );
                } catch (IllegalArgumentException exception) {
                    throw new IllegalArgumentException(
                            "Invalid payment status: " + status
                    );
                }
            }

            return paymentService.getMyPayments(
                    page,
                    size,
                    paymentStatus
            );
        }
    }
