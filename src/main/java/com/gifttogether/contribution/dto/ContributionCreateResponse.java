package com.gifttogether.contribution.dto;

import com.gifttogether.contribution.domain.Contribution;
import com.gifttogether.contribution.domain.ContributionStatus;
import com.gifttogether.funding.domain.FundingStatus;

public record ContributionCreateResponse(
        Long contributionId,
        Long fundingId,
        Long contributorId,
        Long amount,
        ContributionStatus contributionStatus,
        Long currentAmount,
        Long remainingAmount,
        FundingStatus fundingStatus
) {
    public static ContributionCreateResponse from(Contribution contribution) {
        return new ContributionCreateResponse(
                contribution.getId(),
                contribution.getFunding().getId(),
                contribution.getContributor().getId(),
                contribution.getAmount(),
                contribution.getStatus(),
                contribution.getFunding().getCurrentAmount(),
                contribution.getFunding().getTargetAmount()
                        - contribution.getFunding().getCurrentAmount(),
                contribution.getFunding().getStatus()
        );
    }
}