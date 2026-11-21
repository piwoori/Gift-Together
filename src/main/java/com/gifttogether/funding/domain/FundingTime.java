package com.gifttogether.funding.domain;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class FundingTime {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private FundingTime() {
    }

    // Funding deadlines and new funding, contribution and payment records use Seoul wall time.
    public static LocalDateTime now() {
        return now(Clock.systemUTC());
    }

    public static LocalDateTime now(Clock clock) {
        return LocalDateTime.ofInstant(clock.instant(), SEOUL);
    }
}
