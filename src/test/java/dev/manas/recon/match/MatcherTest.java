package dev.manas.recon.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class MatcherTest {
    private static Txn t(String ref, String amt, String ccy, String date) {
        return new Txn(ref, new BigDecimal(amt), ccy, LocalDate.parse(date));
    }

    private static final MatchConfig TOL = new MatchConfig(new BigDecimal("0.05"), 2);

    @Test
    void exactMatchPairsEqualLines() {
        var r = Matcher.match(List.of(t("T1", "100.00", "USD", "2026-01-01")),
                List.of(t("T1", "100.00", "USD", "2026-01-01")), MatchConfig.STRICT);
        assertEquals(1, r.exactCount());
        assertTrue(r.unmatchedA().isEmpty() && r.unmatchedB().isEmpty());
    }

    @Test
    void trailingZerosDoNotMatterAndReferenceIsCaseInsensitive() {
        var r = Matcher.match(List.of(t("abc", "10.0", "usd", "2026-01-01")),
                List.of(t("ABC", "10.00", "USD", "2026-01-01")), MatchConfig.STRICT);
        assertEquals(1, r.exactCount());
    }

    @Test
    void differentCurrencyNeverMatches() {
        var r = Matcher.match(List.of(t("T1", "100", "USD", "2026-01-01")),
                List.of(t("T1", "100", "EUR", "2026-01-01")), TOL);
        assertEquals(0, r.matches().size());
        assertEquals(1, r.unmatchedA().size());
        assertEquals(1, r.unmatchedB().size());
    }

    @Test
    void duplicatesMatchOneToOne() {
        var a = List.of(t("T1", "5", "USD", "2026-01-01"), t("T1", "5", "USD", "2026-01-01"), t("T1", "5", "USD", "2026-01-01"));
        var b = List.of(t("T1", "5", "USD", "2026-01-01"), t("T1", "5", "USD", "2026-01-01"));
        var r = Matcher.match(a, b, MatchConfig.STRICT);
        assertEquals(2, r.exactCount());
        assertEquals(1, r.unmatchedA().size());
        assertTrue(r.unmatchedB().isEmpty());
    }

    @Test
    void toleranceMatchWithinAmountAndDate() {
        var r = Matcher.match(List.of(t("T1", "100.00", "USD", "2026-01-01")),
                List.of(t("T1", "100.03", "USD", "2026-01-03")), TOL);
        assertEquals(0, r.exactCount());
        assertEquals(1, r.toleranceCount());
        assertFalse(r.matches().get(0).exact());
    }

    @Test
    void outsideToleranceStaysUnmatched() {
        var amount = Matcher.match(List.of(t("T1", "100.00", "USD", "2026-01-01")),
                List.of(t("T1", "100.06", "USD", "2026-01-01")), TOL);
        assertEquals(0, amount.matches().size());
        var date = Matcher.match(List.of(t("T1", "100.00", "USD", "2026-01-01")),
                List.of(t("T1", "100.00", "USD", "2026-01-04")), TOL);
        assertEquals(0, date.matches().size());
    }

    @Test
    void toleranceBoundariesAreInclusive() {
        var r = Matcher.match(List.of(t("T1", "100.00", "USD", "2026-01-01")),
                List.of(t("T1", "100.05", "USD", "2026-01-03")), TOL);
        assertEquals(1, r.toleranceCount());
    }

    @Test
    void exactPassWinsOverToleranceCandidate() {
        var a = List.of(t("T1", "100.00", "USD", "2026-01-01"));
        var b = List.of(t("T1", "100.02", "USD", "2026-01-01"), t("T1", "100.00", "USD", "2026-01-01"));
        var r = Matcher.match(a, b, TOL);
        assertEquals(1, r.exactCount());
        assertEquals(new BigDecimal("100.00"), r.matches().get(0).b().amount());
        assertEquals(1, r.unmatchedB().size());
    }

    @Test
    void closestToleranceCandidateIsChosen() {
        var a = List.of(t("T1", "100.00", "USD", "2026-01-01"));
        var b = List.of(t("T1", "100.04", "USD", "2026-01-01"), t("T1", "100.01", "USD", "2026-01-02"));
        var r = Matcher.match(a, b, TOL);
        assertEquals(new BigDecimal("100.01"), r.matches().get(0).b().amount());
        assertEquals(1, r.unmatchedB().size());
    }

    @Test
    void eachLineIsUsedAtMostOnce() {
        var a = List.of(t("T1", "100.00", "USD", "2026-01-01"), t("T1", "100.01", "USD", "2026-01-01"));
        var b = List.of(t("T1", "100.02", "USD", "2026-01-01"));
        var r = Matcher.match(a, b, TOL);
        assertEquals(1, r.matches().size());
        assertEquals(1, r.unmatchedA().size());
    }

    @Test
    void emptyInputsAreFine() {
        var r = Matcher.match(List.of(), List.of(), TOL);
        assertTrue(r.matches().isEmpty());
        var one = Matcher.match(List.of(t("T1", "1", "USD", "2026-01-01")), List.of(), TOL);
        assertEquals(1, one.unmatchedA().size());
    }

    @Test
    void resultIsDeterministic() {
        var a = List.of(t("T1", "1", "USD", "2026-01-01"), t("T2", "2", "USD", "2026-01-01"));
        var b = List.of(t("T2", "2", "USD", "2026-01-01"), t("T1", "1", "USD", "2026-01-01"));
        assertEquals(Matcher.match(a, b, TOL), Matcher.match(a, b, TOL));
    }

    @Test
    void largeInputsStayFast() {
        int n = 100_000;
        var a = new java.util.ArrayList<Txn>(n);
        var b = new java.util.ArrayList<Txn>(n);
        for (int i = 0; i < n; i++) {
            a.add(t("R" + i, "10." + (i % 100 < 10 ? "0" : "") + (i % 100), "USD", "2026-01-01"));
            b.add(t("R" + (n - 1 - i), "10." + ((n - 1 - i) % 100 < 10 ? "0" : "") + ((n - 1 - i) % 100), "USD", "2026-01-01"));
        }
        long start = System.nanoTime();
        var r = Matcher.match(a, b, TOL);
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertEquals(n, r.exactCount());
        assertTrue(ms < 5_000, "took " + ms + " ms");
    }

    @Test
    void configRejectsNegativeValues() {
        assertThrows(IllegalArgumentException.class, () -> new MatchConfig(new BigDecimal("-1"), 0));
        assertThrows(IllegalArgumentException.class, () -> new MatchConfig(BigDecimal.ZERO, -1));
    }
}
