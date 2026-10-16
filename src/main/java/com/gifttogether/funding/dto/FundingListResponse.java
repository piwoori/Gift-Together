package com.gifttogether.funding.dto;

import com.gifttogether.funding.domain.Funding;
import com.gifttogether.funding.domain.FundingStatus;

public record FundingListResponse(
        Long fundingId,
        String productName,
        String productImageUrl,
        Long targetAmount,
        Long currentAmount,
        FundingStatus status
) {
    public static FundingListResponse from(Funding funding) {
        return new FundingListResponse(
                funding.getId(),
                funding.getProduct().getName(),
                funding.getProduct().getImageUrl(),
                funding.getTargetAmount(),
                funding.getCurrentAmount(),
                funding.getStatus()
        );
    }
}