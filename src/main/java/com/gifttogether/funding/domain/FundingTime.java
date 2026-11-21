package com.gifttogether.funding.domain;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class FundingTime {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private FundingTime() {
    }

    // Existing API and database timestamps store Seoul wall time without an offset.
    public static LocalDateTime now(Clock clock) {
        return LocalDateTime.ofInstant(clock.instant(), SEOUL);
    }
}
