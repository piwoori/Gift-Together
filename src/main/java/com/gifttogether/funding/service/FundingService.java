package com.gifttogether.funding.service;

import java.util.List;
import java.time.LocalDateTime;

import com.gifttogether.funding.domain.Funding;
import com.gifttogether.funding.dto.FundingCreateRequest;
import com.gifttogether.funding.dto.FundingCreateResponse;
import com.gifttogether.funding.repository.FundingRepository;
import com.gifttogether.product.domain.Product;
import com.gifttogether.product.repository.ProductRepository;
import com.gifttogether.user.domain.User;
import com.gifttogether.user.repository.UserRepository;
import com.gifttogether.wishlist.repository.WishlistItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.gifttogether.funding.dto.FundingDetailResponse;
import com.gifttogether.contribution.domain.Contribution;
import com.gifttogether.contribution.domain.ContributionStatus;
import com.gifttogether.contribution.repository.ContributionRepository;
import com.gifttogether.payment.domain.Payment;
import com.gifttogether.payment.repository.PaymentRepository;
import com.gifttogether.wallet.domain.Wallet;
import com.gifttogether.wallet.domain.WalletTransaction;
import com.gifttogether.wallet.repository.WalletRepository;
import com.gifttogether.wallet.repository.WalletTransactionRepository;
import com.gifttogether.funding.domain.FundingStatus;

@Service
@RequiredArgsConstructor
public class FundingService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final FundingRepository fundingRepository;
    private final ContributionRepository contributionRepository;
    private final PaymentRepository paymentRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    @Transactional(readOnly = true)
    public FundingDetailResponse getFunding(Long fundingId) {
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new IllegalArgumentException("펀딩을 찾을 수 없습니다."));

        return FundingDetailResponse.from(funding);
    }

    @Transactional
    public FundingCreateResponse createFunding(
            Long userId,
            FundingCreateRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        boolean exists = wishlistItemRepository
                .existsByUserIdAndProductId(userId, request.productId());

        if (!exists) {
            throw new IllegalArgumentException("위시리스트에 없는 상품입니다.");
        }

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        Funding funding = new Funding(
                user,
                product,
                request.expiredAt(),
                request.message()
        );

        Funding savedFunding = fundingRepository.save(funding);

        return FundingCreateResponse.from(savedFunding);
    }

    @Transactional
    public void cancelFunding(Long fundingId, Long userId) {

        Funding funding = fundingRepository.findByIdWithLock(fundingId)
                .orElseThrow(() ->
                        new IllegalArgumentException("펀딩을 찾을 수 없습니다."));

        if (!funding.getReceiver().getId().equals(userId)) {
            throw new IllegalStateException("펀딩 생성자만 취소할 수 있습니다.");
        }

        List<Contribution> contributions =
                contributionRepository.findAllByFundingIdAndStatus(
                        fundingId,
                        ContributionStatus.COMPLETED
                );

        for (Contribution contribution : contributions) {

            Payment payment = paymentRepository
                    .findByContributionId(contribution.getId())
                    .orElseThrow(() ->
                            new IllegalArgumentException("결제 내역을 찾을 수 없습니다."));

            payment.cancel();
            contribution.refund();
        }

        funding.cancel();
    }

    @Transactional
    public void expireFunding(Long fundingId, LocalDateTime now) {

        Funding funding = fundingRepository.findByIdWithLock(fundingId)
                .orElseThrow(() ->
                        new IllegalArgumentException("펀딩을 찾을 수 없습니다."));

        if (funding.getStatus() == FundingStatus.EXPIRED) {
            return;
        }

        funding.expire(now);

        Long amount = funding.getCurrentAmount();

        if (amount == 0) {
            return;
        }

        Wallet wallet = walletRepository
                .findByUserId(funding.getReceiver().getId())
                .orElseGet(() ->
                        walletRepository.save(
                                new Wallet(funding.getReceiver())
                        )
                );

        wallet.deposit(amount);

        WalletTransaction transaction =
                new WalletTransaction(wallet, funding, amount);

        walletTransactionRepository.save(transaction);
    }
}