package com.gifttogether.contribution.dto;

import com.gifttogether.contribution.domain.Contribution;
import com.gifttogether.contribution.domain.ContributionStatus;

public record ContributionResponse(
        Long contributionId,
        String contributorNickname,
        Long amount,
        ContributionStatus status,
        boolean anonymous
) {

    public static ContributionResponse from(Contribution contribution) {

        if (contribution.isAnonymous()) {
            return new ContributionResponse(
                    contribution.getId(),
                    "익명",
                    contribution.getAmount(),
                    contribution.getStatus(),
                    true
            );
        }

        return new ContributionResponse(
                contribution.getId(),
                contribution.getContributor().getNickname(),
                contribution.getAmount(),
                contribution.getStatus(),
                false
        );
    }
}