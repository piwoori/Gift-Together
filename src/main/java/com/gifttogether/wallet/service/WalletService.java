package com.gifttogether.wallet.service;

import com.gifttogether.user.domain.User;
import com.gifttogether.user.repository.UserRepository;
import com.gifttogether.wallet.domain.Wallet;
import com.gifttogether.wallet.dto.WalletResponse;
import com.gifttogether.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

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
}