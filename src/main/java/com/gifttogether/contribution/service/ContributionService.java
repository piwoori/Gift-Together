package com.gifttogether.contribution.service;

import com.gifttogether.common.exception.BadRequestException;
import com.gifttogether.common.exception.ConflictException;
import com.gifttogether.common.exception.ForbiddenException;
import com.gifttogether.contribution.domain.Contribution;
import com.gifttogether.contribution.domain.ContributionIdempotency;
import com.gifttogether.contribution.dto.ContributionCreateRequest;
import com.gifttogether.contribution.dto.ContributionCreateResponse;
import com.gifttogether.contribution.dto.ContributionResponse;
import com.gifttogether.contribution.dto.ContributionAttemptResponse;
import com.gifttogether.contribution.repository.ContributionIdempotencyRepository;
import com.gifttogether.contribution.repository.ContributionRepository;
import com.gifttogether.common.exception.ContributionNotFoundException;
import com.gifttogether.common.exception.FundingNotFoundException;
import com.gifttogether.common.exception.UserNotFoundException;
import com.gifttogether.funding.domain.Funding;
import com.gifttogether.funding.repository.FundingRepository;
import com.gifttogether.user.domain.User;
import com.gifttogether.user.repository.UserRepository;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.gifttogether.payment.domain.Payment;
import com.gifttogether.payment.repository.PaymentRepository;
import com.gifttogether.funding.domain.FundingTime;
import com.gifttogether.contribution.dto.MyContributionResponse;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ContributionService {

    private static final Pattern IDEMPOTENCY_KEY = Pattern.compile("[A-Za-z0-9._~-]{1,128}");

    private final Clock clock;
    private final Validator validator;
    private final ContributionIdempotencyRepository idempotencyRepository;
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
        return contribute(fundingId, userId, request, null);
    }

    @Transactional
    public ContributionCreateResponse contribute(
            Long fundingId,
            Long userId,
            ContributionCreateRequest request,
            String idempotencyKey
    ) {
        if (idempotencyKey == null) {
            validateRequest(request);
        } else {
            validateIdempotencyKey(idempotencyKey);
        }

        Funding funding = fundingRepository.findByIdWithLock(fundingId)
                .orElseThrow(FundingNotFoundException::new);

        User contributor = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        if (idempotencyKey != null) {
            // The funding lock serializes the lookup and insertion for this scope.
            var previous = idempotencyRepository.findByUserIdAndFundingIdAndIdempotencyKey(
                    userId, fundingId, idempotencyKey);
            if (previous.isPresent()) {
                var stored = previous.get();
                if (!stored.matches(request)) {
                    throw new ConflictException("IDEMPOTENCY_KEY_CONFLICT",
                            "이미 다른 참여 요청에 사용된 Idempotency-Key입니다.");
                }
                return stored.response();
            }
            validateRequest(request);
        }

        var response = createContribution(funding, contributor, request);
        if (idempotencyKey != null) {
            idempotencyRepository.save(new ContributionIdempotency(idempotencyKey, request, response));
        }
        return response;
    }

    @Transactional(readOnly = true)
    public Optional<ContributionAttemptResponse> findAttempt(Long fundingId, Long userId, String key) {
        validateIdempotencyKey(key);
        return idempotencyRepository.findByUserIdAndFundingIdAndIdempotencyKey(userId, fundingId, key)
                .map(attempt -> new ContributionAttemptResponse(attempt.request(), attempt.response()));
    }

    private void validateIdempotencyKey(String key) {
        if (key == null || !IDEMPOTENCY_KEY.matcher(key).matches()) {
            throw new BadRequestException("Idempotency-Key는 영문, 숫자 또는 . _ ~ - 문자로 1~128자여야 합니다.");
        }
    }

    private void validateRequest(ContributionCreateRequest request) {
        validator.validate(request).stream().findFirst().ifPresent(violation -> {
            throw new BadRequestException(violation.getMessage());
        });
    }

    private ContributionCreateResponse createContribution(
            Funding funding, User contributor, ContributionCreateRequest request
    ) {
        LocalDateTime now = FundingTime.now(clock);
        if (request.simulatePaymentFailure()) {
            funding.validateContribution(request.amount(), now);
        } else {
            funding.contribute(request.amount(), now);
        }

        Contribution contribution = new Contribution(
                funding,
                contributor,
                request.amount(),
                request.anonymous(),
                now
        );

        Contribution savedContribution =
                contributionRepository.save(contribution);

        Payment payment = new Payment(
                savedContribution,
                request.amount()
        );

        if (request.simulatePaymentFailure()) {
            payment.fail();
            savedContribution.fail(now);
            paymentRepository.save(payment);

            return ContributionCreateResponse.from(savedContribution);
        }

        payment.success(now);
        paymentRepository.save(payment);

        savedContribution.complete(now);

        return ContributionCreateResponse.from(savedContribution);
    }

    @Transactional
    public void cancelContribution(
            Long fundingId,
            Long contributionId,
            Long userId
    ) {
        Contribution contribution = contributionRepository
                .findById(contributionId)
                .orElseThrow(ContributionNotFoundException::new);

        // URL의 fundingId와 실제 참여의 fundingId가 일치하는지 확인
        if (!contribution.getFunding().getId().equals(fundingId)) {
            throw new ContributionNotFoundException();
        }

        if (!contribution.getContributor().getId().equals(userId)) {
            throw new ForbiddenException(
                    "본인의 참여만 취소할 수 있습니다."
            );
        }

        Funding funding = fundingRepository
                .findByIdWithLock(fundingId)
                .orElseThrow(FundingNotFoundException::new);

        LocalDateTime now = FundingTime.now(clock);
        funding.validateContributionCancellation(now);

        Payment payment = paymentRepository
                .findByContributionId(contributionId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "결제 내역을 찾을 수 없습니다."
                        )
                );

        payment.cancel(now);
        funding.cancelContribution(contribution.getAmount(), now);
        contribution.cancel(now);
    }

    @Transactional(readOnly = true)
    public List<ContributionResponse> getContributions(Long fundingId) {

        if (!fundingRepository.existsById(fundingId)) {
            throw new FundingNotFoundException();
        }

        return contributionRepository
                .findAllByFundingId(fundingId)
                .stream()
                .map(ContributionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MyContributionResponse> getMyContributions(
            Long fundingId,
            Long userId
    ) {
        if (!fundingRepository.existsById(fundingId)) {
            throw new FundingNotFoundException();
        }

        return contributionRepository
                .findAllByFundingIdAndContributorId(fundingId, userId)
                .stream()
                .map(MyContributionResponse::from)
                .toList();
    }
}
