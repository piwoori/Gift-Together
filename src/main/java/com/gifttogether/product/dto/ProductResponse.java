package com.gifttogether.product.dto;

import com.gifttogether.product.domain.Product;

public record ProductResponse(
        Long productId,
        String name,
        Long price,
        String imageUrl
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getImageUrl()
        );
    }
}