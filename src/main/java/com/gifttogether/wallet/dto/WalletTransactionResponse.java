package com.gifttogether.wallet.dto;

import com.gifttogether.wallet.domain.WalletTransaction;
import com.gifttogether.wallet.domain.WalletTransactionType;

import java.time.LocalDateTime;

public record WalletTransactionResponse(
        Long transactionId,
        Long fundingId,
        Long amount,
        WalletTransactionType type,
        LocalDateTime createdAt
) {
    public static WalletTransactionResponse from(
            WalletTransaction transaction
    ) {
        return new WalletTransactionResponse(
                transaction.getId(),
                transaction.getFunding().getId(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getCreatedAt()
        );
    }
}