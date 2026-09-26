package com.gifttogether.contribution.repository;

import com.gifttogether.contribution.domain.Contribution;
import com.gifttogether.contribution.domain.ContributionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContributionRepository
        extends JpaRepository<Contribution, Long> {

    List<Contribution> findAllByFundingId(
            Long fundingId
    );

    List<Contribution> findAllByFundingIdAndStatus(
            Long fundingId,
            ContributionStatus status
    );
}