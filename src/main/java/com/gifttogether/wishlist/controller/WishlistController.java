package com.gifttogether.wishlist.controller;

import com.gifttogether.wishlist.dto.WishlistResponse;
import com.gifttogether.wishlist.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public List<WishlistResponse> getWishlist(
            @RequestHeader("X-USER-ID") Long userId
    ) {
        return wishlistService.getWishlist(userId);
    }
}