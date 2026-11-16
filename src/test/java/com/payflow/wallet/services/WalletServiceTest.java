
package com.payflow.wallet.service;

import com.payflow.auth.repository.UserRepository;
import com.payflow.security.CustomUserDetails;
import com.payflow.wallet.entity.Wallet;
import com.payflow.wallet.entity.WalletTransaction;
import com.payflow.wallet.repository.WalletRepository;
import com.payflow.wallet.repository.WalletTransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @InjectMocks
    private WalletService walletService;

    private Wallet wallet;

    @BeforeEach
    void setUp() {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getId()).thenReturn(1L);

        var authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        wallet = new Wallet();
        wallet.setBalance(new BigDecimal("701.00"));
        wallet.setCurrency("INR");

        when(walletRepository.findByUserIdForUpdate(1L))
                .thenReturn(Optional.of(wallet));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void creditWalletShouldIncreaseBalance() {
        when(walletTransactionRepository.save(any(WalletTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WalletTransaction transaction = walletService.creditWallet(
                new BigDecimal("100.00"),
                WalletTransaction.ReferenceType.REFUND,
                33L,
                "Test refund credit"
        );

        assertEquals(
                0,
                new BigDecimal("801.00").compareTo(wallet.getBalance())
        );

        assertEquals(
                0,
                new BigDecimal("701.00")
                        .compareTo(transaction.getBalanceBefore())
        );

        assertEquals(
                0,
                new BigDecimal("801.00")
                        .compareTo(transaction.getBalanceAfter())
        );

        assertEquals(
                WalletTransaction.TransactionType.CREDIT,
                transaction.getType()
        );

        verify(walletRepository).save(wallet);
        verify(walletTransactionRepository)
                .save(any(WalletTransaction.class));
    }

    @Test
    void debitWalletShouldDecreaseBalance() {
        when(walletTransactionRepository.save(any(WalletTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WalletTransaction transaction = walletService.debitWallet(
                new BigDecimal("100.00"),
                WalletTransaction.ReferenceType.PAYMENT,
                33L,
                "Test payment debit"
        );

        assertEquals(
                0,
                new BigDecimal("601.00").compareTo(wallet.getBalance())
        );

        assertEquals(
                0,
                new BigDecimal("701.00")
                        .compareTo(transaction.getBalanceBefore())
        );

        assertEquals(
                0,
                new BigDecimal("601.00")
                        .compareTo(transaction.getBalanceAfter())
        );

        assertEquals(
                WalletTransaction.TransactionType.DEBIT,
                transaction.getType()
        );

        verify(walletRepository).save(wallet);
        verify(walletTransactionRepository)
                .save(any(WalletTransaction.class));
    }

    @Test
    void debitWalletShouldRejectInsufficientBalance() {
        assertThrows(
                IllegalArgumentException.class,
                () -> walletService.debitWallet(
                        new BigDecimal("800.00"),
                        WalletTransaction.ReferenceType.PAYMENT,
                        33L,
                        "Insufficient balance test"
                )
        );

        assertEquals(
                0,
                new BigDecimal("701.00").compareTo(wallet.getBalance())
        );

        verify(walletRepository, never()).save(any(Wallet.class));

        verify(walletTransactionRepository, never())
                .save(any(WalletTransaction.class));
    }
}
