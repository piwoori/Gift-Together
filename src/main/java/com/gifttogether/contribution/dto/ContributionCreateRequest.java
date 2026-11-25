package com.gifttogether.contribution.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;

public record ContributionCreateRequest(
        @NotNull
        @Positive
        @JsonDeserialize(using = ContributionAmountDeserializer.class)
        Long amount,

        boolean anonymous,

        boolean simulatePaymentFailure
) {
}