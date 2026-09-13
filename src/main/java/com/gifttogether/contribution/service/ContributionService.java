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
import com.gifttogether.payment.domain.Payment;
import com.gifttogether.payment.repository.PaymentRepository;

@Service
@RequiredArgsConstructor
public class ContributionService {

    private final FundingRepository fundingRepository;
    private final UserRepository userRepository;
    private final ContributionRepository contributionRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public ContributionCreateResponse contribute(
            Long fundingId,
            Long userId,
            ContributionCreateRequest request
    ) {
        Funding funding = fundingRepository.findByIdWithLock(fundingId)
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

        Contribution savedContribution =
                contributionRepository.save(contribution);

        Payment payment = new Payment(
                savedContribution,
                request.amount()
        );

        if (request.simulatePaymentFailure()) {
            payment.fail();
            paymentRepository.save(payment);

            return ContributionCreateResponse.from(savedContribution);
        }

        payment.success();
        paymentRepository.save(payment);

        funding.contribute(request.amount());

        savedContribution.complete();

        return ContributionCreateResponse.from(savedContribution);
    }
}