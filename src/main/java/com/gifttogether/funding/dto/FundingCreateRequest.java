package com.gifttogether.funding.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record FundingCreateRequest(

        @NotNull
        Long productId,

        @NotNull
        LocalDateTime expiredAt,

        String message
) {
}