package com.payflow.refund.service;

import com.payflow.auth.entity.User;
import com.payflow.common.exception.PaymentNotFoundException;
import com.payflow.common.exception.RefundException;
import com.payflow.payment.entity.Payment;
import com.payflow.payment.entity.PaymentStatus;
import com.payflow.payment.repository.PaymentRepository;
import com.payflow.refund.dto.RefundResponse;
import com.payflow.refund.entity.Refund;
import com.payflow.refund.entity.RefundStatus;
import com.payflow.refund.repository.RefundRepository;
import com.payflow.security.CustomUserDetails;
import com.payflow.transaction.entity.Transaction;
import com.payflow.transaction.entity.TransactionStatus;
import com.payflow.transaction.entity.TransactionType;
import com.payflow.transaction.repository.TransactionRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
public class RefundService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final TransactionRepository transactionRepository;

    public RefundService(
            PaymentRepository paymentRepository,
            RefundRepository refundRepository,
            TransactionRepository transactionRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public RefundResponse createRefund(
            Long paymentId,
            BigDecimal amount
    ) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        Long authenticatedUserId = userDetails.getId();

        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() ->
                        new PaymentNotFoundException(
                                "Payment not found: " + paymentId
                        )
                );

        User paymentUser = payment.getUser();

        if (!paymentUser.getId().equals(authenticatedUserId)) {
            throw new PaymentNotFoundException(
                    "Payment not found: " + paymentId
            );
        }

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new RefundException(
                    "Only successful payments can be refunded"
            );
        }

        if (refundRepository.existsByPaymentId(paymentId)) {
            throw new RefundException(
                    "Payment has already been refunded"
            );
        }

        Refund refund = new Refund();

        refund.setPayment(payment);
        refund.setAmount(payment.getAmount());
        refund.setStatus(RefundStatus.SUCCESS);

        refund.setProviderRefundId(
                "mock_refund_" + payment.getId()
        );

        Refund savedRefund =
                refundRepository.save(refund);

        payment.setStatus(PaymentStatus.REFUNDED);

        Payment updatedPayment =
                paymentRepository.save(payment);

        Transaction transaction = new Transaction();

        transaction.setPayment(updatedPayment);
        transaction.setType(TransactionType.REFUND);
        transaction.setAmount(savedRefund.getAmount());
        transaction.setCurrency(updatedPayment.getCurrency());
        transaction.setStatus(TransactionStatus.SUCCESS);

        transactionRepository.save(transaction);

        return toResponse(savedRefund);
    }

    private RefundResponse toResponse(Refund refund) {

        return new RefundResponse(
                refund.getId(),
                refund.getPayment().getId(),
                refund.getAmount(),
                refund.getStatus(),
                refund.getProviderRefundId(),
                refund.getCreatedAt(),
                refund.getUpdatedAt()
        );
    }
}