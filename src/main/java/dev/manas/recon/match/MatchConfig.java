package dev.manas.recon.match;

import java.math.BigDecimal;

/** Tolerances applied in the second matching pass. */
public record MatchConfig(BigDecimal amountTolerance, int dateToleranceDays) {
    public static final MatchConfig STRICT = new MatchConfig(BigDecimal.ZERO, 0);

    public MatchConfig {
        if (amountTolerance == null || amountTolerance.signum() < 0)
            throw new IllegalArgumentException("amountTolerance must be >= 0");
        if (dateToleranceDays < 0) throw new IllegalArgumentException("dateToleranceDays must be >= 0");
    }
}
