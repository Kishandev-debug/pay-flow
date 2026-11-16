
package com.payflow.wallet.controller;

import com.payflow.wallet.dto.WalletResponse;
import com.payflow.wallet.entity.Wallet;
import com.payflow.wallet.service.WalletService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.payflow.wallet.entity.WalletTransaction;
import com.payflow.wallet.dto.WalletTransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestParam;
@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WalletResponse createWallet() {
        Wallet wallet = walletService.createWalletForCurrentUser();
        return WalletResponse.from(wallet);
    }

    @GetMapping
    public WalletResponse getWallet() {
        Wallet wallet = walletService.getCurrentUserWallet();
        return WalletResponse.from(wallet);
    }

    @GetMapping("/transactions")
    public Page<WalletTransactionResponse> getWalletTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false)
            WalletTransaction.TransactionType type
    ) {
        return walletService.getCurrentUserTransactions(
                page, size, type
        );
    }

}
