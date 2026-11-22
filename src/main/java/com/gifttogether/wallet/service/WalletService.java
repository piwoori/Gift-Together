package com.gifttogether.wallet.service;

import com.gifttogether.funding.domain.Funding;
import com.gifttogether.user.domain.User;
import com.gifttogether.user.repository.UserRepository;
import com.gifttogether.wallet.domain.Wallet;
import com.gifttogether.wallet.domain.WalletTransaction;
import com.gifttogether.wallet.dto.WalletResponse;
import com.gifttogether.wallet.repository.WalletRepository;
import com.gifttogether.wallet.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.gifttogether.wallet.dto.WalletTransactionResponse;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    @Transactional
    public WalletResponse getWallet(Long userId) {

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseGet(() -> getOrCreateWalletWithUserLock(userId));

        return WalletResponse.from(wallet);
    }

    // The caller already holds the funding lock.
    @Transactional(propagation = Propagation.MANDATORY)
    public void creditExpiredFunding(Funding funding, Long amount) {
        Wallet wallet = getOrCreateWalletWithUserLock(funding.getReceiver().getId());
        wallet.deposit(amount);
        walletTransactionRepository.save(new WalletTransaction(wallet, funding, amount));
    }

    private Wallet getOrCreateWalletWithUserLock(Long userId) {
        User user = userRepository.findByIdWithLock(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return walletRepository.findByUserId(userId)
                .orElseGet(() -> walletRepository.save(new Wallet(user)));
    }

    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> getTransactions(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElse(null);

        if (wallet == null) {
            return List.of();
        }

        return walletTransactionRepository
                .findAllByWalletIdOrderByCreatedAtDesc(wallet.getId())
                .stream()
                .map(WalletTransactionResponse::from)
                .toList();
    }
}