package com.gifttogether.wallet.controller;

import com.gifttogether.wallet.dto.WalletResponse;
import com.gifttogether.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public ResponseEntity<WalletResponse> getWallet(
            @RequestHeader("X-USER-ID") Long userId
    ) {

        WalletResponse response =
                walletService.getWallet(userId);

        return ResponseEntity.ok(response);
    }
}