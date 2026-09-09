package com.gifttogether.contribution.controller;

import com.gifttogether.contribution.dto.ContributionCreateRequest;
import com.gifttogether.contribution.dto.ContributionCreateResponse;
import com.gifttogether.contribution.service.ContributionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fundings/{fundingId}/contributions")
@RequiredArgsConstructor
public class ContributionController {

    private final ContributionService contributionService;

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
}