
package com.gifttogether.auth.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        Long userId,
        String nickname
) {
}
