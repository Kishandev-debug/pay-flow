
package com.payflow.wallet.service;

import com.payflow.security.CustomUserDetails;
import com.payflow.wallet.entity.Wallet;
import com.payflow.wallet.entity.WalletTransaction;
import com.payflow.wallet.repository.WalletRepository;
import com.payflow.wallet.repository.WalletTransactionRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
class WalletServiceIntegrationTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void creditWalletShouldUpdateRealDatabaseAndCreateLedgerEntry() {
        // Existing local development wallet belongs to user ID 6.
        Long userId = 6L;

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new AssertionError("Wallet not found for user ID " + userId));

        BigDecimal originalBalance = wallet.getBalance();

        CustomUserDetails principal = mock(CustomUserDetails.class);
        when(principal.getId()).thenReturn(userId);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities() == null
                        ? java.util.Collections.emptyList()
                        : principal.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);

        BigDecimal amount = new BigDecimal("100.00");

        WalletTransaction transaction = walletService.creditWallet(
                amount,
                WalletTransaction.ReferenceType.ADJUSTMENT,
                null,
                "Integration test wallet credit"
        );

        Wallet updatedWallet = walletRepository.findByUserId(userId)
                .orElseThrow();

        assertEquals(
                0,
                updatedWallet.getBalance().compareTo(originalBalance.add(amount)),
                "Wallet balance should increase by ₹100"
        );

        assertNotNull(transaction.getId(), "Ledger transaction should be persisted");
        assertEquals(
                0,
                transaction.getAmount().compareTo(amount)
        );
        assertEquals(
                0,
                transaction.getBalanceBefore().compareTo(originalBalance)
        );
        assertEquals(
                0,
                transaction.getBalanceAfter()
                        .compareTo(originalBalance.add(amount))
        );
        assertEquals(
                WalletTransaction.TransactionType.CREDIT,
                transaction.getType()
        );

        var ledgerEntries = walletTransactionRepository
                .findByWalletIdOrderByCreatedAtDesc(
                        wallet.getId(), PageRequest.of(0, 10));

        assertTrue(
                ledgerEntries.getContent().stream()
                        .anyMatch(entry ->
                                transaction.getId().equals(entry.getId())),
                "Credit entry should exist in the PostgreSQL ledger"
        );
    }
}
