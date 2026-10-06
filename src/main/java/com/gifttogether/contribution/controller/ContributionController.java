package com.gifttogether.contribution.controller;

import com.gifttogether.contribution.dto.ContributionCreateRequest;
import com.gifttogether.contribution.dto.ContributionCreateResponse;
import com.gifttogether.contribution.dto.ContributionResponse;
import com.gifttogether.contribution.service.ContributionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fundings/{fundingId}/contributions")
@Tag(
        name = "Contribution",
        description = "펀딩 참여·참여 내역 조회·참여 취소 API"
)
public class ContributionController {

    private final ContributionService contributionService;

    @Operation(
            summary = "펀딩 참여",
            description = "진행 중인 펀딩에 원하는 금액만큼 참여합니다."
    )
    @PostMapping
    public ResponseEntity<ContributionCreateResponse> contribute(
            @PathVariable Long fundingId,
            @RequestHeader("X-USER-ID") Long userId,
            @Valid @RequestBody ContributionCreateRequest request
    ) {
        ContributionCreateResponse response =
                contributionService.contribute(
                        fundingId,
                        userId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(
            summary = "펀딩 참여 취소",
            description = "본인의 참여를 취소하고 결제 금액을 환불합니다. 진행 중인 펀딩에서만 가능합니다."
    )
    @PostMapping("/{contributionId}/cancel")
    public ResponseEntity<Void> cancelContribution(
            @PathVariable Long fundingId,
            @PathVariable Long contributionId,
            @RequestHeader("X-USER-ID") Long userId
    ) {
        contributionService.cancelContribution(
                fundingId,
                contributionId,
                userId
        );

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "참여 내역 조회",
            description = "펀딩의 참여 내역을 조회합니다. 익명 참여자의 개인정보는 노출되지 않습니다."
    )
    @GetMapping
    public List<ContributionResponse> getContributions(
            @PathVariable Long fundingId
    ) {
        return contributionService.getContributions(fundingId);
    }
}