package com.gifttogether.contribution.domain;

import com.gifttogether.contribution.dto.ContributionCreateRequest;
import com.gifttogether.contribution.dto.ContributionCreateResponse;
import com.gifttogether.funding.domain.FundingStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Table(name = "contribution_idempotency", uniqueConstraints = @UniqueConstraint(
        name = "uk_contribution_idempotency_scope",
        columnNames = {"user_id", "funding_id", "idempotency_key"}
))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContributionIdempotency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long fundingId;

    @Column(nullable = false, length = 128)
    private String idempotencyKey;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private boolean anonymous;

    @Column(nullable = false)
    private boolean simulatePaymentFailure;

    @Column(nullable = false)
    private Long contributionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContributionStatus contributionStatus;

    @Column(nullable = false)
    private Long currentAmount;

    @Column(nullable = false)
    private Long remainingAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FundingStatus fundingStatus;

    public ContributionIdempotency(String key, ContributionCreateRequest request,
                                   ContributionCreateResponse response) {
        this.userId = response.contributorId();
        this.fundingId = response.fundingId();
        this.idempotencyKey = key;
        this.amount = response.amount();
        this.anonymous = request.anonymous();
        this.simulatePaymentFailure = request.simulatePaymentFailure();
        this.contributionId = response.contributionId();
        this.contributionStatus = response.contributionStatus();
        this.currentAmount = response.currentAmount();
        this.remainingAmount = response.remainingAmount();
        this.fundingStatus = response.fundingStatus();
    }

    public boolean matches(ContributionCreateRequest request) {
        return Objects.equals(amount, request.amount())
                && anonymous == request.anonymous()
                && simulatePaymentFailure == request.simulatePaymentFailure();
    }

    public ContributionCreateResponse response() {
        return new ContributionCreateResponse(contributionId, fundingId, userId, amount,
                contributionStatus, currentAmount, remainingAmount, fundingStatus);
    }
}
