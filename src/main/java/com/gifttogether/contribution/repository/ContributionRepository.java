package com.gifttogether.contribution.repository;

import java.util.List;

import com.gifttogether.contribution.domain.Contribution;
import org.springframework.data.jpa.repository.JpaRepository;
import com.gifttogether.contribution.domain.ContributionStatus;


public interface ContributionRepository
        extends JpaRepository<Contribution, Long> {

    List<Contribution> findAllByFundingIdAndStatus(
            Long fundingId,
            ContributionStatus status
    );
}