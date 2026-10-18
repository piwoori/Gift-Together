
package com.gifttogether.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // 기존 테스트 및 더미 사용자 생성 호환용
    public User(String nickname, String email) {
        this.nickname = nickname;
        this.email = email;
        this.createdAt = LocalDateTime.now();
    }

    // 신규 회원가입용
    public User(String nickname, String email, String passwordHash) {
        this(nickname, email);
        this.passwordHash = passwordHash;
    }
}
