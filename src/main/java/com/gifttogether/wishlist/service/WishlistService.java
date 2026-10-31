package com.gifttogether.wishlist.service;

import com.gifttogether.common.exception.*;
import com.gifttogether.user.repository.UserRepository;
import com.gifttogether.wishlist.dto.WishlistResponse;
import com.gifttogether.wishlist.repository.WishlistItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.gifttogether.product.domain.Product;
import com.gifttogether.product.repository.ProductRepository;
import com.gifttogether.user.domain.User;
import com.gifttogether.wishlist.domain.WishlistItem;
import com.gifttogether.common.exception.ForbiddenException;
import com.gifttogether.common.exception.WishlistItemNotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<WishlistResponse> getWishlist(Long userId) {

        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException();
        }

        return wishlistItemRepository
                .findAllByUserId(userId)
                .stream()
                .map(WishlistResponse::from)
                .toList();
    }

    @Transactional
    public void deleteWishlistItem(Long userId, Long wishlistItemId) {

        WishlistItem wishlistItem = wishlistItemRepository
                .findById(wishlistItemId)
                .orElseThrow(WishlistItemNotFoundException::new);

        if (!wishlistItem.getUser().getId().equals(userId)) {
            throw new ForbiddenException("본인의 위시리스트 상품만 삭제할 수 있습니다.");
        }

        wishlistItemRepository.delete(wishlistItem);
    }

    @Transactional
    public void addWishlistItem(Long userId, Long productId) {

        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        Product product = productRepository.findById(productId)
                .orElseThrow(ProductNotFoundException::new);

        if (wishlistItemRepository.existsByUserIdAndProductId(
                userId, productId
        )) {
            throw new ConflictException(
                    "이미 위시리스트에 등록된 상품입니다."
            );
        }

        WishlistItem wishlistItem = new WishlistItem(user, product);

        wishlistItemRepository.save(wishlistItem);
    }
}