package com.gifttogether.contribution.domain;

import com.gifttogether.funding.domain.Funding;
import com.gifttogether.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "contributions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Contribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funding_id", nullable = false)
    private Funding funding;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contributor_id", nullable = false)
    private User contributor;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContributionStatus status;

    @Column(nullable = false)
    private boolean anonymous;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Contribution(
            Funding funding,
            User contributor,
            Long amount,
            boolean anonymous
    ) {
        this.funding = funding;
        this.contributor = contributor;
        this.amount = amount;
        this.anonymous = anonymous;
        this.status = ContributionStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = ContributionStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }
}