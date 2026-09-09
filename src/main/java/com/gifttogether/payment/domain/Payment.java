package com.gifttogether.payment.domain;

import com.gifttogether.contribution.domain.Contribution;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contribution_id", nullable = false, unique = true)
    private Contribution contribution;

    @Column(nullable = false, unique = true, length = 100)
    private String paymentKey;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    private LocalDateTime paidAt;

    private LocalDateTime cancelledAt;

    public Payment(Contribution contribution, Long amount) {
        this.contribution = contribution;
        this.amount = amount;
        this.paymentKey = UUID.randomUUID().toString();
        this.status = PaymentStatus.READY;
    }

    public void success() {
        if (this.status != PaymentStatus.READY) {
            throw new IllegalStateException("결제 대기 상태가 아닙니다.");
        }

        this.status = PaymentStatus.SUCCESS;
        this.paidAt = LocalDateTime.now();
    }

    public void fail() {
        if (this.status != PaymentStatus.READY) {
            throw new IllegalStateException("결제 대기 상태가 아닙니다.");
        }

        this.status = PaymentStatus.FAILED;
    }

    public void cancel() {
        if (this.status != PaymentStatus.SUCCESS) {
            throw new IllegalStateException("성공한 결제만 취소할 수 있습니다.");
        }

        this.status = PaymentStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }
}