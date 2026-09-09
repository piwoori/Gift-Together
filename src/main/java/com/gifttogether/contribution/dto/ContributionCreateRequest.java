package com.gifttogether.contribution.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ContributionCreateRequest(
        @NotNull
        @Min(1000)
        Long amount,

        boolean anonymous
) {
}