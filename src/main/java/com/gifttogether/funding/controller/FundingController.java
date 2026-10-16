package com.gifttogether.funding.controller;

import com.gifttogether.funding.dto.FundingCreateRequest;
import com.gifttogether.funding.dto.FundingCreateResponse;
import com.gifttogether.funding.dto.FundingDetailResponse;
import com.gifttogether.funding.dto.FundingListResponse;
import com.gifttogether.funding.service.FundingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fundings")
@RequiredArgsConstructor
@Tag(
        name = "Funding",
        description = "공동 선물 펀딩 생성·조회·취소 API"
)
public class FundingController {

    private final FundingService fundingService;

    @Operation(
            summary = "내 펀딩 목록 조회",
            description = "현재 사용자가 생성한 공동 선물 펀딩을 최신순으로 조회합니다."
    )
    @GetMapping("/my")
    public ResponseEntity<List<FundingListResponse>> getMyFundings(
            @RequestHeader("X-USER-ID") Long userId
    ) {
        List<FundingListResponse> response =
                fundingService.getMyFundings(userId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{fundingId}")
    public ResponseEntity<FundingDetailResponse> getFunding(
            @PathVariable Long fundingId
    ) {
        FundingDetailResponse response =
                fundingService.getFunding(fundingId);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "펀딩 생성",
            description = "위시리스트에 등록된 상품으로 공동 선물 펀딩을 생성합니다."
    )
    @PostMapping
    public ResponseEntity<FundingCreateResponse> createFunding(
            @RequestHeader("X-USER-ID") Long userId,
            @Valid @RequestBody FundingCreateRequest request
    ) {
        FundingCreateResponse response =
                fundingService.createFunding(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(
            summary = "펀딩 취소",
            description = "펀딩 생성자가 진행 중인 펀딩을 취소하고 참여 금액을 환불합니다."
    )
    @PostMapping("/{fundingId}/cancel")
    public ResponseEntity<Void> cancelFunding(
            @PathVariable Long fundingId,
            @RequestHeader("X-USER-ID") Long userId
    ) {
        fundingService.cancelFunding(fundingId, userId);

        return ResponseEntity.noContent().build();
    }
}