package com.gifttogether.funding.scheduler;

import com.gifttogether.funding.domain.Funding;
import com.gifttogether.funding.domain.FundingTime;
import com.gifttogether.funding.domain.FundingStatus;
import com.gifttogether.funding.repository.FundingRepository;
import com.gifttogether.funding.service.FundingService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FundingExpirationScheduler {

    private final Clock clock;
    private final FundingRepository fundingRepository;
    private final FundingService fundingService;

    @Scheduled(fixedDelay = 60000)
    public void expireFundings() {

        LocalDateTime now = FundingTime.now(clock);

        List<Funding> expiredFundings =
                fundingRepository.findAllByStatusAndExpiredAtLessThanEqual(
                        FundingStatus.OPEN,
                        now
                );

        for (Funding funding : expiredFundings) {
            fundingService.expireFunding(
                    funding.getId(),
                    now
            );
        }
    }
}