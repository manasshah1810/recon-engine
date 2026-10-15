package dev.manas.recon.match;

import java.util.List;

public record MatchResult(List<MatchPair> matches, List<Txn> unmatchedA, List<Txn> unmatchedB) {
    public long exactCount() {
        return matches.stream().filter(MatchPair::exact).count();
    }

    public long toleranceCount() {
        return matches.stream().filter(m -> !m.exact()).count();
    }
}
