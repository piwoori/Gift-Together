package com.gifttogether.funding.dto;

import com.gifttogether.funding.domain.Funding;
import com.gifttogether.funding.domain.FundingStatus;

import java.time.LocalDateTime;

public record FundingDetailResponse(
        Long fundingId,
        Long receiverId,
        String receiverNickname,
        Long productId,
        String productName,
        String productImageUrl,
        Long targetAmount,
        Long currentAmount,
        Long remainingAmount,
        FundingStatus status,
        String message,
        LocalDateTime expiredAt
) {

    public static FundingDetailResponse from(Funding funding) {
        return new FundingDetailResponse(
                funding.getId(),
                funding.getReceiver().getId(),
                funding.getReceiver().getNickname(),
                funding.getProduct().getId(),
                funding.getProduct().getName(),
                funding.getProduct().getImageUrl(),
                funding.getTargetAmount(),
                funding.getCurrentAmount(),
                funding.getTargetAmount() - funding.getCurrentAmount(),
                funding.getStatus(),
                funding.getMessage(),
                funding.getExpiredAt()
        );
    }
}