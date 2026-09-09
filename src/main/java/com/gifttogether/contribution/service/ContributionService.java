package com.gifttogether.contribution.service;

import com.gifttogether.contribution.domain.Contribution;
import com.gifttogether.contribution.dto.ContributionCreateRequest;
import com.gifttogether.contribution.dto.ContributionCreateResponse;
import com.gifttogether.contribution.repository.ContributionRepository;
import com.gifttogether.funding.domain.Funding;
import com.gifttogether.funding.repository.FundingRepository;
import com.gifttogether.user.domain.User;
import com.gifttogether.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContributionService {

    private final FundingRepository fundingRepository;
    private final UserRepository userRepository;
    private final ContributionRepository contributionRepository;

    @Transactional
    public ContributionCreateResponse contribute(
            Long fundingId,
            Long userId,
            ContributionCreateRequest request
    ) {
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() ->
                        new IllegalArgumentException("펀딩을 찾을 수 없습니다."));

        User contributor = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Contribution contribution = new Contribution(
                funding,
                contributor,
                request.amount(),
                request.anonymous()
        );

        // 일단 Mock Payment가 성공했다고 가정
        contribution.complete();

        // 펀딩 현재 금액 증가
        funding.contribute(request.amount());

        Contribution savedContribution =
                contributionRepository.save(contribution);

        return ContributionCreateResponse.from(savedContribution);
    }
}