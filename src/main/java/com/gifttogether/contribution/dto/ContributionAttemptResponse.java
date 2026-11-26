package com.gifttogether.contribution.dto;

public record ContributionAttemptResponse(
        ContributionCreateRequest request,
        ContributionCreateResponse result
) {
}
