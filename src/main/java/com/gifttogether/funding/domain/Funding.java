package com.gifttogether.funding.domain;

import com.gifttogether.product.domain.Product;
import com.gifttogether.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "fundings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Funding {

    public void contribute(Long amount) {
        if (this.status != FundingStatus.OPEN) {
            throw new IllegalStateException("진행 중인 펀딩이 아닙니다.");
        }

        if (LocalDateTime.now().isAfter(this.expiredAt)) {
            throw new IllegalStateException("마감된 펀딩입니다.");
        }

        if (amount < 1000) {
            throw new IllegalArgumentException("최소 참여 금액은 1,000원입니다.");
        }

        long remainingAmount = this.targetAmount - this.currentAmount;

        if (amount > remainingAmount) {
            throw new IllegalArgumentException("남은 금액을 초과할 수 없습니다.");
        }

        this.currentAmount += amount;

        if (this.currentAmount.equals(this.targetAmount)) {
            this.status = FundingStatus.COMPLETED;
        }

        this.updatedAt = LocalDateTime.now();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Long targetAmount;

    @Column(nullable = false)
    private Long currentAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FundingStatus status;

    @Column(length = 500)
    private String message;

    @Column(nullable = false)
    private LocalDateTime expiredAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Funding(
            User receiver,
            Product product,
            LocalDateTime expiredAt,
            String message
    ) {
        this.receiver = receiver;
        this.product = product;
        this.targetAmount = product.getPrice();
        this.currentAmount = 0L;
        this.status = FundingStatus.OPEN;
        this.message = message;
        this.expiredAt = expiredAt;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}