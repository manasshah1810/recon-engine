package dev.manas.recon.stream;

import dev.manas.recon.domain.ReconRun;
import dev.manas.recon.match.MatchConfig;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/streams")
public class StreamController {
    private final EventIngestService service;

    public StreamController(EventIngestService service) {
        this.service = service;
    }

    /** Same ingestion path as the Kafka listener, useful without a broker. */
    @PostMapping("/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> ingest(@RequestBody String json) {
        return Map.of("outcome", service.ingest(json).name());
    }

    /** Reconcile everything received so far for a stream. */
    @PostMapping("/{streamId}/reconcile")
    @ResponseStatus(HttpStatus.CREATED)
    public ReconRun reconcile(@PathVariable String streamId,
                              @RequestParam(defaultValue = "0") BigDecimal amountTolerance,
                              @RequestParam(defaultValue = "0") int dateToleranceDays) {
        return service.reconcileStream(streamId, new MatchConfig(amountTolerance, dateToleranceDays));
    }
}
