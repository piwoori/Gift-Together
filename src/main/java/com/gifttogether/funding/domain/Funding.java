package com.gifttogether.funding.domain;

import com.gifttogether.common.exception.BadRequestException;
import com.gifttogether.common.exception.ConflictException;
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
        contribute(amount, FundingTime.now());
    }

    public void contribute(Long amount, LocalDateTime now) {
        validateContribution(amount, now);

        this.currentAmount += amount;

        if (this.currentAmount.equals(this.targetAmount)) {
            this.status = FundingStatus.COMPLETED;
        }

        this.updatedAt = now;
    }

    public void validateContribution(Long amount, LocalDateTime now) {
        requireOpenBeforeDeadline(now, "진행 중인 펀딩이 아닙니다.");

        if (amount < 1000) {
            throw new BadRequestException("최소 참여 금액은 1,000원입니다.");
        }

        long remainingAmount =
                this.targetAmount - this.currentAmount;

        if (amount > remainingAmount) {
            throw new BadRequestException(
                    "남은 금액을 초과할 수 없습니다."
            );
        }
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
        this(receiver, product, expiredAt, message, FundingTime.now());
    }

    public Funding(User receiver, Product product, LocalDateTime expiredAt, String message, LocalDateTime now) {
        this.receiver = receiver;
        this.product = product;
        this.targetAmount = product.getPrice();
        this.currentAmount = 0L;
        this.status = FundingStatus.OPEN;
        this.message = message;
        this.expiredAt = expiredAt;
        this.createdAt = now;
        this.updatedAt = this.createdAt;
    }

    public void validateContributionCancellation(LocalDateTime now) {
        requireOpenBeforeDeadline(now, "진행 중인 펀딩의 참여만 취소할 수 있습니다.");
    }

    public void cancelContribution(Long amount, LocalDateTime now) {
        validateContributionCancellation(now);

        if (amount <= 0) {
            throw new BadRequestException("취소 금액은 0원보다 커야 합니다.");
        }

        if (amount > this.currentAmount) {
            throw new BadRequestException("현재 모금액보다 큰 금액을 취소할 수 없습니다.");
        }

        this.currentAmount -= amount;
        this.updatedAt = now;
    }

    public void cancel() {
        cancel(FundingTime.now());
    }

    public void cancel(LocalDateTime now) {
        requireOpenBeforeDeadline(now, "진행 중인 펀딩만 취소할 수 있습니다.");

        this.status = FundingStatus.CANCELLED;
        this.currentAmount = 0L;
        this.updatedAt = now;
    }

    private void requireOpenBeforeDeadline(LocalDateTime now, String invalidStatusMessage) {
        if (this.status != FundingStatus.OPEN) {
            throw new ConflictException(invalidStatusMessage);
        }
        if (!now.isBefore(expiredAt)) {
            throw new ConflictException("마감된 펀딩입니다.");
        }
    }

    public void expire(LocalDateTime now) {
        if (this.status != FundingStatus.OPEN) {
            throw new ConflictException("진행 중인 펀딩만 만료할 수 있습니다.");
        }

        if (now.isBefore(this.expiredAt)) {
            throw new ConflictException("아직 마감 시간이 지나지 않았습니다.");
        }

        this.status = FundingStatus.EXPIRED;
        this.updatedAt = now;
    }
}