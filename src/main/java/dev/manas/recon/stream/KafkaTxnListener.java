package dev.manas.recon.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Consumes transaction events. Enabled with recon.kafka.enabled=true so the app also runs without a broker. */
@Component
@ConditionalOnProperty(name = "recon.kafka.enabled", havingValue = "true")
public class KafkaTxnListener {
    private static final Logger log = LoggerFactory.getLogger(KafkaTxnListener.class);
    private final EventIngestService ingest;

    public KafkaTxnListener(EventIngestService ingest) {
        this.ingest = ingest;
    }

    @KafkaListener(topics = "${recon.kafka.topic}")
    public void onMessage(String payload) {
        try {
            if (ingest.ingest(payload) == EventIngestService.Outcome.DUPLICATE) log.debug("duplicate event ignored");
        } catch (DataIntegrityViolationException e) {
            log.debug("duplicate event ignored (concurrent insert)");
        } catch (IllegalArgumentException e) {
            // A malformed message must not block the partition. Skipped and logged; no dead-letter topic yet.
            log.warn("skipping invalid event: {}", e.getMessage());
        }
    }
}
