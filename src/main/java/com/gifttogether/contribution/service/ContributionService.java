package com.gifttogether.contribution.service;

import com.gifttogether.common.exception.ForbiddenException;
import com.gifttogether.contribution.domain.Contribution;
import com.gifttogether.contribution.dto.ContributionCreateRequest;
import com.gifttogether.contribution.dto.ContributionCreateResponse;
import com.gifttogether.contribution.repository.ContributionRepository;
import com.gifttogether.common.exception.ConflictException;
import com.gifttogether.common.exception.ContributionNotFoundException;
import com.gifttogether.common.exception.ForbiddenException;
import com.gifttogether.common.exception.FundingNotFoundException;
import com.gifttogether.common.exception.UserNotFoundException;
import com.gifttogether.funding.domain.Funding;
import com.gifttogether.funding.repository.FundingRepository;
import com.gifttogether.user.domain.User;
import com.gifttogether.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.gifttogether.payment.domain.Payment;
import com.gifttogether.payment.repository.PaymentRepository;
import com.gifttogether.funding.domain.FundingStatus;

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
                .orElseThrow(FundingNotFoundException::new);

        User contributor = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

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

    @Transactional
    public void cancelContribution(Long contributionId, Long userId) {

        Contribution contribution =
                contributionRepository.findById(contributionId)
                        .orElseThrow(ContributionNotFoundException::new);

        if (!contribution.getContributor().getId().equals(userId)) {
            throw new ForbiddenException("본인의 참여만 취소할 수 있습니다.");
        }

        Funding funding = fundingRepository
                .findByIdWithLock(contribution.getFunding().getId())
                .orElseThrow(FundingNotFoundException::new);

        if (funding.getStatus() != FundingStatus.OPEN) {
            throw new ConflictException(
                    "진행 중인 펀딩에서만 참여를 취소할 수 있습니다."
            );
        }

        Payment payment = paymentRepository
                .findByContributionId(contributionId)
                .orElseThrow(() ->
                        new IllegalArgumentException("결제 내역을 찾을 수 없습니다."));

        payment.cancel();
        funding.cancelContribution(contribution.getAmount());
        contribution.cancel();
    }
}