package com.gifttogether.wallet.domain;

import com.gifttogether.funding.domain.Funding;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "wallet_transactions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_wallet_transaction_funding_type",
                        columnNames = {"funding_id", "type"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funding_id", nullable = false)
    private Funding funding;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WalletTransactionType type;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public WalletTransaction(
            Wallet wallet,
            Funding funding,
            Long amount
    ) {
        this.wallet = wallet;
        this.funding = funding;
        this.amount = amount;
        this.type = WalletTransactionType.FUNDING_EXPIRED_REWARD;
        this.createdAt = LocalDateTime.now();
    }
}