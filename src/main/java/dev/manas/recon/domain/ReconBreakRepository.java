package dev.manas.recon.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconBreakRepository extends JpaRepository<ReconBreak, Long> {
    List<ReconBreak> findByRunIdOrderByIdAsc(Long runId);

    List<ReconBreak> findByRunIdAndStatusOrderByIdAsc(Long runId, ReconBreak.Status status);

    List<ReconBreak> findByRunIdAndTypeOrderByIdAsc(Long runId, ReconBreak.Type type);

    List<ReconBreak> findByRunIdAndStatusAndTypeOrderByIdAsc(Long runId, ReconBreak.Status status, ReconBreak.Type type);
}
