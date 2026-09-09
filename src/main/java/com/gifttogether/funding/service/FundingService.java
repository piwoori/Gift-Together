package com.gifttogether.funding.service;

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

@Service
@RequiredArgsConstructor
public class FundingService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final FundingRepository fundingRepository;

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
}