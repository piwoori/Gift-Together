package com.gifttogether.contribution.repository;

import com.gifttogether.contribution.domain.ContributionIdempotency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContributionIdempotencyRepository extends JpaRepository<ContributionIdempotency, Long> {

    Optional<ContributionIdempotency> findByUserIdAndFundingIdAndIdempotencyKey(
            Long userId, Long fundingId, String idempotencyKey
    );
}
