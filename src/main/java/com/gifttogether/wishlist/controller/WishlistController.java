package com.gifttogether.wishlist.controller;

import com.gifttogether.wishlist.dto.WishlistResponse;
import com.gifttogether.wishlist.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/wishlist")
@Tag(
        name = "Wishlist",
        description = "사용자 위시리스트 조회 API"
)
public class WishlistController {

    private final WishlistService wishlistService;

    @Operation(
            summary = "위시리스트 조회",
            description = "사용자의 위시리스트에 등록된 상품 목록을 조회합니다."
    )
    @GetMapping
    public List<WishlistResponse> getWishlist(
            @RequestHeader("X-USER-ID") Long userId
    ) {
        return wishlistService.getWishlist(userId);
    }
}