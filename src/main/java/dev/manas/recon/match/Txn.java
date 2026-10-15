package dev.manas.recon.match;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One line from a source file. Immutable. */
public record Txn(String reference, BigDecimal amount, String currency, LocalDate date) {
    public Txn {
        if (reference == null || reference.isBlank()) throw new IllegalArgumentException("reference is required");
        if (amount == null) throw new IllegalArgumentException("amount is required");
        if (currency == null || currency.trim().length() != 3) throw new IllegalArgumentException("currency must be a 3-letter code");
        if (date == null) throw new IllegalArgumentException("date is required");
        reference = reference.trim();
        currency = currency.trim().toUpperCase();
    }

    String normalizedReference() {
        return reference.toUpperCase();
    }
}
