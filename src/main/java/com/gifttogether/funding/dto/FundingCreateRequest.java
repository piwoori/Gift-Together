package com.gifttogether.funding.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record FundingCreateRequest(

        @NotNull
        Long productId,

        @NotNull
        @JsonDeserialize(using = FundingDeadlineDeserializer.class)
        LocalDateTime expiredAt,

        String message
) {
}