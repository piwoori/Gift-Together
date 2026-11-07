package com.gifttogether.contribution.dto;

import com.gifttogether.contribution.domain.Contribution;
import com.gifttogether.contribution.domain.ContributionStatus;

public record MyContributionResponse(
        Long contributionId,
        String contributorNickname,
        Long amount,
        ContributionStatus status,
        boolean anonymous
) {
    public static MyContributionResponse from(Contribution contribution) {
        return new MyContributionResponse(
                contribution.getId(),
                contribution.isAnonymous()
                        ? "익명"
                        : contribution.getContributor().getNickname(),
                contribution.getAmount(),
                contribution.getStatus(),
                contribution.isAnonymous()
        );
    }
}