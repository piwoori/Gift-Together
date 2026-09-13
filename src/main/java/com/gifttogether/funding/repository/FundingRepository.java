package com.gifttogether.funding.repository;

import com.gifttogether.funding.domain.Funding;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FundingRepository extends JpaRepository<Funding, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Funding f where f.id = :fundingId")
    Optional<Funding> findByIdWithLock(
            @Param("fundingId") Long fundingId
    );
}