package com.gifttogether.wishlist.dto;

import com.gifttogether.wishlist.domain.WishlistItem;

public record WishlistResponse(
        Long wishlistItemId,
        Long productId,
        String productName,
        Long price,
        String imageUrl
) {

    public static WishlistResponse from(WishlistItem wishlistItem) {
        return new WishlistResponse(
                wishlistItem.getId(),
                wishlistItem.getProduct().getId(),
                wishlistItem.getProduct().getName(),
                wishlistItem.getProduct().getPrice(),
                wishlistItem.getProduct().getImageUrl()
        );
    }
}