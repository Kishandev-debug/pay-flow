
package com.payflow.refund.contoller;

import com.payflow.refund.dto.RefundRequest;
import com.payflow.refund.dto.RefundResponse;
import com.payflow.refund.service.RefundService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/refunds")
public class RefundController {

    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @PostMapping("/{paymentId}")
    public RefundResponse createRefund(
            @PathVariable Long paymentId,
            @Valid @RequestBody RefundRequest request
    ) {
        return refundService.createRefund(
                paymentId,
                request.getAmount()
        );
    }
}
