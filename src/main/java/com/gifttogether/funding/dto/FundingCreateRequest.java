package com.gifttogether.funding.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record FundingCreateRequest(

        @NotNull
        Long productId,

        @NotNull
        @Future
        LocalDateTime expiredAt,

        String message
) {
}