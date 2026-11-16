
package com.payflow.wallet.service;

import com.payflow.wallet.dto.WalletDepositRequest;
import com.payflow.wallet.entity.Wallet;
import com.payflow.wallet.entity.WalletDeposit;
import com.payflow.wallet.entity.WalletTransaction;
import com.payflow.wallet.repository.WalletDepositRepository;
import com.payflow.wallet.repository.WalletRepository;
import com.payflow.wallet.repository.WalletTransactionRepository;
import com.payflow.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class WalletDepositService {

    private final WalletRepository walletRepository;
    private final WalletDepositRepository walletDepositRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletService walletService;

    public WalletDepositService(
            WalletRepository walletRepository,
            WalletDepositRepository walletDepositRepository,
            WalletTransactionRepository walletTransactionRepository,
            WalletService walletService
    ) {
        this.walletRepository = walletRepository;
        this.walletDepositRepository = walletDepositRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.walletService = walletService;
    }

    @Transactional
    public WalletDeposit createPendingDeposit(WalletDepositRequest request) {
        Long userId = getAuthenticatedUserId();

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Wallet not found. Create a wallet first."
                        ));

        if (!"INR".equalsIgnoreCase(wallet.getCurrency())) {
            throw new IllegalArgumentException(
                    "Deposits are currently supported only for INR wallets"
            );
        }

        WalletDeposit deposit = new WalletDeposit();
        deposit.setWallet(wallet);
        deposit.setAmount(request.getAmount());
        deposit.setCurrency("INR");
        deposit.setProvider("MOCK");
        deposit.setProviderReference("mock_deposit_" + UUID.randomUUID());
        deposit.setStatus(WalletDeposit.DepositStatus.PENDING);

        return walletDepositRepository.save(deposit);
    }

    /**
     * Development-only simulation.
     * This method must not be exposed as an unrestricted production endpoint.
     */
    @Transactional
    public WalletDeposit confirmMockDeposit(Long depositId) {
        Long userId = getAuthenticatedUserId();

        WalletDeposit deposit = walletDepositRepository
                .findByIdForUpdate(depositId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Deposit not found"));

        if (!"MOCK".equals(deposit.getProvider())) {
            throw new IllegalArgumentException(
                    "Only mock deposits can use mock confirmation"
            );
        }

        if (!deposit.getWallet().getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Deposit not found");
        }

        if (deposit.getStatus() != WalletDeposit.DepositStatus.PENDING) {
            throw new IllegalStateException(
                    "Deposit is not pending; current status: "
                            + deposit.getStatus()
            );
        }

        // Mark VERIFIED within the same transaction.
        deposit.setStatus(WalletDeposit.DepositStatus.VERIFIED);
        walletDepositRepository.saveAndFlush(deposit);

        // Credits the authenticated user's wallet and creates a ledger entry.
        walletService.creditVerifiedDeposit(
                deposit.getAmount(),
                deposit.getId(),
                "Mock wallet deposit " + deposit.getId()
        );

        deposit.setStatus(WalletDeposit.DepositStatus.CREDITED);

        return walletDepositRepository.save(deposit);
    }

    private Long getAuthenticatedUserId() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                instanceof CustomUserDetails)) {
            throw new IllegalStateException("User is not authenticated");
        }

        return ((CustomUserDetails) authentication.getPrincipal()).getId();
    }
}
