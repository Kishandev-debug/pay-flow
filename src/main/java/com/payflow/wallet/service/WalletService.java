
package com.payflow.wallet.service;

import com.payflow.auth.entity.User;
import com.payflow.auth.repository.UserRepository;
import com.payflow.security.CustomUserDetails;
import com.payflow.wallet.dto.WalletTransactionResponse;
import com.payflow.wallet.entity.Wallet;
import com.payflow.wallet.entity.WalletTransaction;
import com.payflow.wallet.repository.WalletRepository;
import com.payflow.wallet.repository.WalletTransactionRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    public WalletService(
            WalletRepository walletRepository,
            UserRepository userRepository,
            WalletTransactionRepository walletTransactionRepository
    ) {
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
        this.walletTransactionRepository = walletTransactionRepository;
    }

    @Transactional
    public Wallet createWalletForCurrentUser() {
        Long userId = getAuthenticatedUserId();

        if (walletRepository.existsByUserId(userId)) {
            throw new IllegalArgumentException(
                    "Wallet already exists for this user"
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(new BigDecimal("0.00"));
        wallet.setCurrency("INR");

        return walletRepository.save(wallet);
    }

    @Transactional(readOnly = true)
    public Wallet getCurrentUserWallet() {
        Long userId = getAuthenticatedUserId();

        return walletRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Wallet not found. Create your wallet first."
                        )
                );
    }


    @Transactional
    public WalletTransaction creditWallet(
            BigDecimal amount,
            WalletTransaction.ReferenceType referenceType,
            Long referenceId,
            String description
    ) {
        if (referenceType == WalletTransaction.ReferenceType.DEPOSIT) {
            throw new IllegalArgumentException(
                    "Deposit credits must use the verified deposit flow"
            );
        }

        return updateBalance(
                amount,
                WalletTransaction.TransactionType.CREDIT,
                referenceType,
                referenceId,
                description
        );
    }


    @Transactional
    public WalletTransaction debitWallet(
            BigDecimal amount,
            WalletTransaction.ReferenceType referenceType,
            Long referenceId,
            String description
    ) {
        return updateBalance(
                amount,
                WalletTransaction.TransactionType.DEBIT,
                referenceType,
                referenceId,
                description
        );
    }

    /**
     * Credits a deposit only when its reference has not already
     * been recorded.
     *
     * IMPORTANT: This method does not verify a payment provider.
     * Call it only after a trusted server-side deposit verification.
     */
    @Transactional
    public WalletTransaction creditVerifiedDeposit(
            BigDecimal amount,
            Long depositReferenceId,
            String description
    ) {
        validateAmount(amount);

        if (depositReferenceId == null || depositReferenceId <= 0) {
            throw new IllegalArgumentException(
                    "Valid deposit reference ID is required"
            );
        }

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException(
                    "Deposit description is required"
            );
        }

        if (walletTransactionRepository
                .existsByReferenceTypeAndReferenceId(
                        WalletTransaction.ReferenceType.DEPOSIT,
                        depositReferenceId
                )) {
            throw new IllegalArgumentException(
                    "This deposit has already been credited"
            );
        }

        return updateBalance(
                amount,
                WalletTransaction.TransactionType.CREDIT,
                WalletTransaction.ReferenceType.DEPOSIT,
                depositReferenceId,
                description
        );
    }

    private WalletTransaction updateBalance(
            BigDecimal amount,
            WalletTransaction.TransactionType type,
            WalletTransaction.ReferenceType referenceType,
            Long referenceId,
            String description
    ) {
        validateAmount(amount);

        if (type == null) {
            throw new IllegalArgumentException(
                    "Transaction type is required"
            );
        }

        if (referenceType == null) {
            throw new IllegalArgumentException(
                    "Reference type is required"
            );
        }

        Long userId = getAuthenticatedUserId();

        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Wallet not found. Create your wallet first."
                        )
                );

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter;

        if (type == WalletTransaction.TransactionType.CREDIT) {
            balanceAfter = balanceBefore.add(amount);
        } else {
            if (balanceBefore.compareTo(amount) < 0) {
                throw new IllegalArgumentException(
                        "Insufficient wallet balance"
                );
            }

            balanceAfter = balanceBefore.subtract(amount);
        }

        wallet.setBalance(balanceAfter);
        walletRepository.save(wallet);

        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(wallet);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setReferenceType(referenceType);
        transaction.setReferenceId(referenceId);
        transaction.setDescription(description);

        return walletTransactionRepository.save(transaction);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be greater than zero"
            );
        }

        if (amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(
                    "Amount cannot have more than two decimal places"
            );
        }

        if (amount.precision() > 19) {
            throw new IllegalArgumentException(
                    "Amount exceeds the supported limit"
            );
        }
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

    @Transactional(readOnly = true)
    public Page<WalletTransactionResponse> getCurrentUserTransactions(
            int page,
            int size,
            WalletTransaction.TransactionType type
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

        Long userId = getAuthenticatedUserId();

        if (!walletRepository.existsByUserId(userId)) {
            throw new IllegalArgumentException(
                    "Wallet not found. Create your wallet first."
            );
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<WalletTransaction> transactions;

        if (type == null) {
            transactions =
                    walletTransactionRepository
                            .findByWalletUserIdOrderByCreatedAtDesc(
                                    userId, pageable
                            );
        } else {
            transactions =
                    walletTransactionRepository
                            .findByWalletUserIdAndTypeOrderByCreatedAtDesc(
                                    userId, type, pageable
                            );
        }

        return transactions.map(WalletTransactionResponse::from);
    }
}
