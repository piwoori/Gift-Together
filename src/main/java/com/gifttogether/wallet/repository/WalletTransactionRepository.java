package com.gifttogether.wallet.repository;

import com.gifttogether.wallet.domain.WalletTransaction;
import com.gifttogether.wallet.domain.WalletTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletTransactionRepository
        extends JpaRepository<WalletTransaction, Long> {

    boolean existsByFundingIdAndType(
            Long fundingId,
            WalletTransactionType type
    );
}