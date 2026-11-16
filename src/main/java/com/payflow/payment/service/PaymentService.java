
package com.payflow.payment.service;

import com.payflow.auth.entity.User;
import com.payflow.common.exception.IdempotencyConflictException;
import com.payflow.common.exception.PaymentNotFoundException;
import com.payflow.payment.dto.PaymentPageResponse;
import com.payflow.payment.dto.PaymentRequest;
import com.payflow.payment.dto.PaymentResponse;
import com.payflow.payment.entity.Payment;
import com.payflow.payment.entity.PaymentStatus;
import com.payflow.payment.provider.PaymentProvider;
import com.payflow.payment.repository.PaymentRepository;
import com.payflow.security.CustomUserDetails;
import com.payflow.transaction.entity.Transaction;
import com.payflow.transaction.entity.TransactionStatus;
import com.payflow.transaction.entity.TransactionType;
import com.payflow.transaction.repository.TransactionRepository;
import com.payflow.wallet.entity.WalletTransaction;
import com.payflow.wallet.service.WalletService;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentProvider paymentProvider;
    private final TransactionRepository transactionRepository;
    private final WalletService walletService;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentProvider paymentProvider,
            TransactionRepository transactionRepository,
            WalletService walletService
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentProvider = paymentProvider;
        this.transactionRepository = transactionRepository;
        this.walletService = walletService;
    }

    @Transactional
    public PaymentResponse createPayment(
            PaymentRequest request,
            String idempotencyKey
    ) {
        Long authenticatedUserId = getAuthenticatedUserId();

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existingPayment =
                    paymentRepository.findByUserIdAndIdempotencyKey(
                            authenticatedUserId,
                            idempotencyKey
                    );

            if (existingPayment.isPresent()) {
                return toResponse(existingPayment.get());
            }
        }

        Payment payment = new Payment();

        User user = new User();
        user.setId(authenticatedUserId);

        payment.setUser(user);
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency().toUpperCase());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setIdempotencyKey(idempotencyKey);

        boolean walletPayment =
                request.getPaymentMethod() == PaymentRequest.PaymentMethod.WALLET;

        payment.setProvider(
                walletPayment ? "WALLET" : paymentProvider.getProviderName()
        );

        Payment savedPayment;

        try {
            savedPayment = paymentRepository.save(payment);
            paymentRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                var existingPayment =
                        paymentRepository.findByUserIdAndIdempotencyKey(
                                authenticatedUserId,
                                idempotencyKey
                        );

                if (existingPayment.isPresent()) {
                    return toResponse(existingPayment.get());
                }
            }

            throw new IdempotencyConflictException(
                    "Payment could not be created because the idempotency key is already being processed."
            );
        }

        if (walletPayment) {
            if (!"INR".equalsIgnoreCase(savedPayment.getCurrency())) {
                throw new IllegalArgumentException(
                        "Wallet payments currently support INR only"
                );
            }

            walletService.debitWallet(
                    savedPayment.getAmount(),
                    WalletTransaction.ReferenceType.PAYMENT,
                    savedPayment.getId(),
                    "Wallet payment #" + savedPayment.getId()
            );

            savedPayment.setProviderPaymentId(
                    "wallet_payment_" + savedPayment.getId()
            );
        } else {
            String providerPaymentId =
                    paymentProvider.processPayment(savedPayment);

            savedPayment.setProviderPaymentId(providerPaymentId);
        }

        savedPayment.setStatus(PaymentStatus.SUCCESS);

        Payment updatedPayment = paymentRepository.save(savedPayment);

        Transaction transaction = new Transaction();
        transaction.setPayment(updatedPayment);
        transaction.setType(TransactionType.PAYMENT);
        transaction.setAmount(updatedPayment.getAmount());
        transaction.setCurrency(updatedPayment.getCurrency());
        transaction.setStatus(TransactionStatus.SUCCESS);

        transactionRepository.save(transaction);

        return toResponse(updatedPayment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new PaymentNotFoundException(
                                "Payment not found: " + paymentId
                        )
                );

        Long authenticatedUserId = getAuthenticatedUserId();

        if (!payment.getUser().getId().equals(authenticatedUserId)) {
            throw new PaymentNotFoundException(
                    "Payment not found: " + paymentId
            );
        }

        return toResponse(payment);
    }

    /**
     * Gets the authenticated user's payments with pagination.
     */
    @Transactional(readOnly = true)
    public PaymentPageResponse getMyPayments(
            int page,
            int size,
            PaymentStatus status
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

        Long authenticatedUserId = getAuthenticatedUserId();

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Payment> paymentPage;

        if (status != null) {
            paymentPage = paymentRepository.findByUserIdAndStatus(
                    authenticatedUserId,
                    status,
                    pageable
            );
        } else {
            paymentPage = paymentRepository.findByUserId(
                    authenticatedUserId,
                    pageable
            );
        }

        var content = paymentPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return new PaymentPageResponse(
                content,
                paymentPage.getNumber(),
                paymentPage.getSize(),
                paymentPage.getTotalElements(),
                paymentPage.getTotalPages()
        );
    }

    private Long getAuthenticatedUserId() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                instanceof CustomUserDetails userDetails)) {
            throw new IllegalStateException(
                    "Authenticated user not found"
            );
        }

        return userDetails.getId();
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getProvider(),
                payment.getProviderPaymentId(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
