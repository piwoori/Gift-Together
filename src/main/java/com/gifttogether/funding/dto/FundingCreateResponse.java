package com.gifttogether.funding.dto;

import com.gifttogether.funding.domain.Funding;
import com.gifttogether.funding.domain.FundingStatus;

import java.time.LocalDateTime;

public record FundingCreateResponse(
        Long fundingId,
        Long productId,
        Long targetAmount,
        Long currentAmount,
        FundingStatus status,
        String message,
        LocalDateTime expiredAt
) {

    public static FundingCreateResponse from(Funding funding) {
        return new FundingCreateResponse(
                funding.getId(),
                funding.getProduct().getId(),
                funding.getTargetAmount(),
                funding.getCurrentAmount(),
                funding.getStatus(),
                funding.getMessage(),
                funding.getExpiredAt()
        );
    }
}