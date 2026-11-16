
package com.payflow.wallet.controller;

import com.payflow.config.MockDepositProperties;
import com.payflow.wallet.dto.WalletDepositResponse;
import com.payflow.wallet.entity.WalletDeposit;
import com.payflow.wallet.service.WalletDepositService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/dev/wallet/deposits")
@Profile("dev")
public class MockWalletDepositController {

    private final WalletDepositService walletDepositService;
    private final MockDepositProperties mockDepositProperties;

    public MockWalletDepositController(
            WalletDepositService walletDepositService,
            MockDepositProperties mockDepositProperties
    ) {
        this.walletDepositService = walletDepositService;
        this.mockDepositProperties = mockDepositProperties;
    }

    @PostMapping("/{depositId}/confirm")
    public WalletDepositResponse confirmMockDeposit(
            @PathVariable Long depositId
    ) {
        if (!mockDepositProperties.isEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Mock deposit confirmation is disabled"
            );
        }

        WalletDeposit deposit =
                walletDepositService.confirmMockDeposit(depositId);

        return WalletDepositResponse.from(deposit);
    }
}
