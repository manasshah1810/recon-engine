package dev.manas.recon.stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.manas.recon.domain.ReconRun;
import dev.manas.recon.match.MatchConfig;
import dev.manas.recon.match.Txn;
import dev.manas.recon.service.ReconService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stages events from a stream and reconciles a stream on request. Kafka delivery is at-least-once,
 * so ingestion is idempotent: (streamId, eventId) is unique and repeats are ignored.
 */
@Service
public class EventIngestService {
    private final TxnEventRepository events;
    private final ReconService recon;
    private final ObjectMapper mapper;

    public EventIngestService(TxnEventRepository events, ReconService recon, ObjectMapper mapper) {
        this.events = events;
        this.recon = recon;
        this.mapper = mapper;
    }

    public enum Outcome { STORED, DUPLICATE }

    /** Parses one JSON message {streamId,eventId,side,reference,amount,currency,date} and stores it. */
    @Transactional
    public Outcome ingest(String json) {
        TxnEvent e = parse(json);
        if (events.existsByStreamIdAndEventId(e.getStreamId(), e.getEventId())) return Outcome.DUPLICATE;
        events.saveAndFlush(e);
        return Outcome.STORED;
    }

    TxnEvent parse(String json) {
        try {
            JsonNode n = mapper.readTree(json);
            String side = text(n, "side").toUpperCase();
            if (!side.equals("A") && !side.equals("B")) throw new IllegalArgumentException("side must be A or B");
            Txn t = new Txn(text(n, "reference"), new BigDecimal(text(n, "amount")), text(n, "currency"), LocalDate.parse(text(n, "date")));
            return new TxnEvent(text(n, "streamId"), text(n, "eventId"), side, t.reference(), t.amount(), t.currency(), t.date());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("malformed event: " + e.getMessage(), e);
        }
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        if (v == null || v.isNull() || v.asText().isBlank()) throw new IllegalArgumentException(field + " is required");
        return v.asText();
    }

    @Transactional
    public ReconRun reconcileStream(String streamId, MatchConfig cfg) {
        if (events.countByStreamId(streamId) == 0) throw new ReconService.NotFoundException("no events for stream " + streamId);
        return recon.reconcile(load(streamId, "A"), load(streamId, "B"), cfg);
    }

    private List<Txn> load(String streamId, String side) {
        return events.findByStreamIdAndSideOrderByIdAsc(streamId, side).stream()
                .map(e -> new Txn(e.getReference(), e.getAmount(), e.getCurrency(), e.getTradeDate())).toList();
    }
}
