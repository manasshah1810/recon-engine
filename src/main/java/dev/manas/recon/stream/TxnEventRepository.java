package dev.manas.recon.stream;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TxnEventRepository extends JpaRepository<TxnEvent, Long> {
    boolean existsByStreamIdAndEventId(String streamId, String eventId);

    List<TxnEvent> findByStreamIdAndSideOrderByIdAsc(String streamId, String side);

    long countByStreamId(String streamId);
}
