package com.gifttogether.wallet.dto;

import com.gifttogether.wallet.domain.Wallet;

public record WalletResponse(
        Long walletId,
        Long userId,
        Long balance
) {

    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getUser().getId(),
                wallet.getBalance()
        );
    }
}