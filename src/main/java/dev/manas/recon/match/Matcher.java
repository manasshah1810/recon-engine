package dev.manas.recon.match;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure, deterministic two-pass matcher. Every transaction is used at most once.
 * Pass 1: exact on (reference, currency, amount, date). Pass 2: same reference and currency,
 * amount and date within tolerance; the closest candidate wins.
 * Output order follows input order, so identical inputs always give identical results.
 */
public final class Matcher {

    private Matcher() {}

    public static MatchResult match(List<Txn> a, List<Txn> b, MatchConfig cfg) {
        boolean[] usedA = new boolean[a.size()];
        boolean[] usedB = new boolean[b.size()];
        List<MatchPair> matches = new ArrayList<>();

        // Pass 1: exact. Index B by key; each key holds a FIFO of B indexes.
        Map<String, Deque<Integer>> index = new HashMap<>();
        for (int j = 0; j < b.size(); j++) {
            index.computeIfAbsent(exactKey(b.get(j)), k -> new ArrayDeque<>()).add(j);
        }
        for (int i = 0; i < a.size(); i++) {
            Deque<Integer> q = index.get(exactKey(a.get(i)));
            if (q != null && !q.isEmpty()) {
                int j = q.poll();
                usedA[i] = true;
                usedB[j] = true;
                matches.add(new MatchPair(a.get(i), b.get(j), true));
            }
        }

        // Pass 2: tolerance, only if any tolerance is configured. Index leftover B by reference.
        if (cfg.amountTolerance().signum() > 0 || cfg.dateToleranceDays() > 0) {
            Map<String, List<Integer>> byRef = new HashMap<>();
            for (int j = 0; j < b.size(); j++) {
                if (!usedB[j]) byRef.computeIfAbsent(refKey(b.get(j)), k -> new ArrayList<>()).add(j);
            }
            for (int i = 0; i < a.size(); i++) {
                if (usedA[i]) continue;
                Txn ta = a.get(i);
                List<Integer> candidates = byRef.get(refKey(ta));
                if (candidates == null) continue;
                int best = -1;
                BigDecimal bestDiff = null;
                long bestDays = 0;
                for (int j : candidates) {
                    if (usedB[j]) continue;
                    Txn tb = b.get(j);
                    BigDecimal diff = ta.amount().subtract(tb.amount()).abs();
                    long days = Math.abs(ChronoUnit.DAYS.between(ta.date(), tb.date()));
                    if (diff.compareTo(cfg.amountTolerance()) > 0 || days > cfg.dateToleranceDays()) continue;
                    if (best < 0 || diff.compareTo(bestDiff) < 0 || (diff.compareTo(bestDiff) == 0 && days < bestDays)) {
                        best = j;
                        bestDiff = diff;
                        bestDays = days;
                    }
                }
                if (best >= 0) {
                    usedA[i] = true;
                    usedB[best] = true;
                    matches.add(new MatchPair(ta, b.get(best), false));
                }
            }
        }

        List<Txn> unA = new ArrayList<>();
        List<Txn> unB = new ArrayList<>();
        for (int i = 0; i < a.size(); i++) if (!usedA[i]) unA.add(a.get(i));
        for (int j = 0; j < b.size(); j++) if (!usedB[j]) unB.add(b.get(j));
        return new MatchResult(matches, unA, unB);
    }

    private static String exactKey(Txn t) {
        // stripTrailingZeros so 10.0 and 10.00 are the same amount
        return refKey(t) + '|' + t.amount().stripTrailingZeros().toPlainString() + '|' + t.date();
    }

    private static String refKey(Txn t) {
        return t.normalizedReference() + '|' + t.currency();
    }
}
