package com.gifttogether.wallet.service;

import com.gifttogether.user.domain.User;
import com.gifttogether.user.repository.UserRepository;
import com.gifttogether.wallet.domain.Wallet;
import com.gifttogether.wallet.dto.WalletResponse;
import com.gifttogether.wallet.repository.WalletRepository;
import com.gifttogether.wallet.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseGet(() ->
                        walletRepository.save(new Wallet(user))
                );

        return WalletResponse.from(wallet);
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