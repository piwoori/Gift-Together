package com.gifttogether.wallet.controller;

import com.gifttogether.wallet.dto.WalletResponse;
import com.gifttogether.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@Tag(
        name = "Wallet",
        description = "사용자 지갑 조회 API"
)
public class WalletController {

    private final WalletService walletService;

    @Operation(
            summary = "지갑 조회",
            description = "사용자의 현재 지갑 잔액을 조회합니다."
    )
    @GetMapping
    public ResponseEntity<WalletResponse> getWallet(
            @RequestHeader("X-USER-ID") Long userId
    ) {

        WalletResponse response =
                walletService.getWallet(userId);

        return ResponseEntity.ok(response);
    }
}