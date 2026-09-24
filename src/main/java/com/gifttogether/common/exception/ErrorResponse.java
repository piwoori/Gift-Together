package com.gifttogether.common.exception;

public record ErrorResponse(
        String code,
        String message
) {
}