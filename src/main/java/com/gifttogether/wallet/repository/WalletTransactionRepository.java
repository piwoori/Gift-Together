package com.gifttogether.wallet.repository;

import com.gifttogether.wallet.domain.WalletTransaction;
import com.gifttogether.wallet.domain.WalletTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WalletTransactionRepository
        extends JpaRepository<WalletTransaction, Long> {

    List<WalletTransaction> findAllByWalletIdOrderByCreatedAtDesc(Long walletId);

    boolean existsByFundingIdAndType(
            Long fundingId,
            WalletTransactionType type
    );
}