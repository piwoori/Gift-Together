package com.gifttogether.wishlist.repository;

import com.gifttogether.wishlist.domain.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WishlistItemRepository
        extends JpaRepository<WishlistItem, Long> {

    boolean existsByUserIdAndProductId(
            Long userId,
            Long productId
    );

    List<WishlistItem> findAllByUserId(Long userId);
}