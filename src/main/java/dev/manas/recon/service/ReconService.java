package dev.manas.recon.service;

import dev.manas.recon.domain.ReconBreak;
import dev.manas.recon.domain.ReconBreak.Status;
import dev.manas.recon.domain.ReconBreak.Type;
import dev.manas.recon.domain.ReconBreakRepository;
import dev.manas.recon.domain.ReconRun;
import dev.manas.recon.domain.ReconRunRepository;
import dev.manas.recon.match.MatchConfig;
import dev.manas.recon.match.MatchPair;
import dev.manas.recon.match.MatchResult;
import dev.manas.recon.match.Matcher;
import dev.manas.recon.match.Txn;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReconService {
    private final ReconRunRepository runs;
    private final ReconBreakRepository breaks;

    public ReconService(ReconRunRepository runs, ReconBreakRepository breaks) {
        this.runs = runs;
        this.breaks = breaks;
    }

    /** Matches the two sides and stores the run summary plus every item that needs review. */
    @Transactional
    public ReconRun reconcile(List<Txn> a, List<Txn> b, MatchConfig cfg) {
        MatchResult r = Matcher.match(a, b, cfg);
        ReconRun run = runs.save(new ReconRun(cfg.amountTolerance(), cfg.dateToleranceDays(), a.size(), b.size(),
                (int) r.exactCount(), (int) r.toleranceCount(), r.unmatchedA().size(), r.unmatchedB().size()));
        List<ReconBreak> out = new ArrayList<>();
        for (MatchPair p : r.matches()) {
            if (p.exact()) continue;
            ReconBreak br = new ReconBreak(run.getId(), Type.TOLERANCE_MATCH, null, p.a().reference(), p.a().amount(),
                    p.a().currency(), p.a().date());
            br.setCounter(p.b().reference(), p.b().amount(), p.b().date());
            out.add(br);
        }
        for (Txn t : r.unmatchedA()) {
            out.add(new ReconBreak(run.getId(), Type.UNMATCHED_A, "A", t.reference(), t.amount(), t.currency(), t.date()));
        }
        for (Txn t : r.unmatchedB()) {
            out.add(new ReconBreak(run.getId(), Type.UNMATCHED_B, "B", t.reference(), t.amount(), t.currency(), t.date()));
        }
        breaks.saveAll(out);
        return run;
    }

    @Transactional(readOnly = true)
    public ReconRun getRun(Long id) {
        return runs.findById(id).orElseThrow(() -> new NotFoundException("run " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<ReconBreak> listBreaks(Long runId, Status status, Type type) {
        getRun(runId);
        if (status != null && type != null) return breaks.findByRunIdAndStatusAndTypeOrderByIdAsc(runId, status, type);
        if (status != null) return breaks.findByRunIdAndStatusOrderByIdAsc(runId, status);
        if (type != null) return breaks.findByRunIdAndTypeOrderByIdAsc(runId, type);
        return breaks.findByRunIdOrderByIdAsc(runId);
    }

    @Transactional
    public ReconBreak updateBreak(Long id, Status status, String note) {
        ReconBreak br = breaks.findById(id).orElseThrow(() -> new NotFoundException("break " + id + " not found"));
        br.resolve(status, note);
        return br;
    }

    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String m) {
            super(m);
        }
    }
}
