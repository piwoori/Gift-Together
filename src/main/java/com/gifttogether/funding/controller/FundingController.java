package com.gifttogether.funding.controller;

import com.gifttogether.funding.dto.FundingCreateRequest;
import com.gifttogether.funding.dto.FundingCreateResponse;
import com.gifttogether.funding.service.FundingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.gifttogether.funding.dto.FundingDetailResponse;

@RestController
@RequestMapping("/api/fundings")
@RequiredArgsConstructor
public class FundingController {

    private final FundingService fundingService;

    @GetMapping("/{fundingId}")
    public ResponseEntity<FundingDetailResponse> getFunding(
            @PathVariable Long fundingId
    ) {
        FundingDetailResponse response = fundingService.getFunding(fundingId);

        return ResponseEntity.ok(response);
    }

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
}