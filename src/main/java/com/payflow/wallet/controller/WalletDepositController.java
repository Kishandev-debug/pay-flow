
package com.payflow.wallet.controller;

import com.payflow.wallet.dto.WalletDepositRequest;
import com.payflow.wallet.dto.WalletDepositResponse;
import com.payflow.wallet.entity.WalletDeposit;
import com.payflow.wallet.service.WalletDepositService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet/deposits")
public class WalletDepositController {

    private final WalletDepositService walletDepositService;

    public WalletDepositController(
            WalletDepositService walletDepositService
    ) {
        this.walletDepositService = walletDepositService;
    }

    @PostMapping
    public ResponseEntity<WalletDepositResponse> createDeposit(
            @Valid @RequestBody WalletDepositRequest request
    ) {
        WalletDeposit deposit =
                walletDepositService.createPendingDeposit(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(WalletDepositResponse.from(deposit));
    }
}
