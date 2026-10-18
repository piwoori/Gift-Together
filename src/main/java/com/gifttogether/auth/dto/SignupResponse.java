
package com.gifttogether.auth.dto;

public record SignupResponse(
        Long userId,
        String nickname,
        String email
) {
}
