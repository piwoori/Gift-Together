
package com.gifttogether.auth.service;

import com.gifttogether.auth.dto.LoginRequest;
import com.gifttogether.auth.dto.LoginResponse;
import com.gifttogether.auth.jwt.JwtTokenProvider;
import com.gifttogether.auth.dto.SignupRequest;
import com.gifttogether.auth.dto.SignupResponse;
import com.gifttogether.user.domain.User;
import com.gifttogether.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SignupResponse signup(SignupRequest request) {

        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    CONFLICT,
                    "이미 사용 중인 이메일입니다."
            );
        }

        String passwordHash = passwordEncoder.encode(request.password());

        User user = new User(
                request.nickname().trim(),
                email,
                passwordHash
        );

        User savedUser = userRepository.save(user);

        return new SignupResponse(
                savedUser.getId(),
                savedUser.getNickname(),
                savedUser.getEmail()
        );
    }


    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        String email = request.email().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        UNAUTHORIZED,
                        "이메일 또는 비밀번호가 올바르지 않습니다."
                ));

        if (user.getPasswordHash() == null ||
                !passwordEncoder.matches(
                        request.password(),
                        user.getPasswordHash()
                )) {
            throw new ResponseStatusException(
                    UNAUTHORIZED,
                    "이메일 또는 비밀번호가 올바르지 않습니다."
            );
        }

        String accessToken =
                jwtTokenProvider.createAccessToken(user.getId());

        return new LoginResponse(
                accessToken,
                "Bearer",
                user.getId(),
                user.getNickname()
        );
    }

}
