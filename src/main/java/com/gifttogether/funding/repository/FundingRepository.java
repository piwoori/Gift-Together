package com.gifttogether.funding.repository;

import com.gifttogether.funding.domain.Funding;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FundingRepository extends JpaRepository<Funding, Long> {
}