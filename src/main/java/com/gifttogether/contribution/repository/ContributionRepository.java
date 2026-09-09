package com.gifttogether.contribution.repository;

import com.gifttogether.contribution.domain.Contribution;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContributionRepository
        extends JpaRepository<Contribution, Long> {
}