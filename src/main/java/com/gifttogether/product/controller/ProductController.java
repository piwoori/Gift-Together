package com.gifttogether.product.controller;

import com.gifttogether.product.dto.ProductResponse;
import com.gifttogether.product.repository.ProductRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
@Tag(
        name = "Product",
        description = "상품 조회 API"
)
public class ProductController {

    private final ProductRepository productRepository;

    @Operation(
            summary = "상품 목록 조회",
            description = "등록된 상품 목록을 조회합니다."
    )
    @GetMapping
    public List<ProductResponse> getProducts() {
        return productRepository.findAll()
                .stream()
                .map(ProductResponse::from)
                .toList();
    }
}