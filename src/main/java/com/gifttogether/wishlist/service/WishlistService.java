package com.gifttogether.wishlist.service;

import com.gifttogether.common.exception.UserNotFoundException;
import com.gifttogether.user.repository.UserRepository;
import com.gifttogether.wishlist.dto.WishlistResponse;
import com.gifttogether.wishlist.repository.WishlistItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final UserRepository userRepository;

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
}