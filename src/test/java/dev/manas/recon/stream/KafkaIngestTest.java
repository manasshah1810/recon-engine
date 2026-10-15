package dev.manas.recon.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "recon.kafka.enabled=true",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer"})
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = "txn-events")
class KafkaIngestTest {
    @Autowired
    KafkaTemplate<String, String> kafka;
    @Autowired
    TxnEventRepository events;
    @Autowired
    EventIngestService service;
    @Autowired
    MockMvc mvc;

    private static String ev(String stream, String id, String side, String ref, String amt, String date) {
        return String.format("{\"streamId\":\"%s\",\"eventId\":\"%s\",\"side\":\"%s\",\"reference\":\"%s\",\"amount\":\"%s\",\"currency\":\"USD\",\"date\":\"%s\"}",
                stream, id, side, ref, amt, date);
    }

    @Test
    void kafkaEventsAreStoredDeduplicatedAndReconciled() throws Exception {
        String s = "s1";
        kafka.send("txn-events", s, ev(s, "e1", "A", "T1", "10.00", "2026-01-01"));
        kafka.send("txn-events", s, ev(s, "e1", "A", "T1", "10.00", "2026-01-01")); // redelivery
        kafka.send("txn-events", s, "not json at all");                              // poison message
        kafka.send("txn-events", s, ev(s, "e2", "B", "T1", "10.00", "2026-01-01"));
        kafka.send("txn-events", s, ev(s, "e3", "A", "T2", "5.00", "2026-01-01"));
        kafka.flush();

        await().atMost(Duration.ofSeconds(20)).until(() -> events.countByStreamId(s) == 3);
        assertEquals(3, events.countByStreamId(s)); // duplicate and poison message did not add rows

        mvc.perform(post("/api/streams/" + s + "/reconcile"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exactMatches").value(1))
                .andExpect(jsonPath("$.unmatchedA").value(1))
                .andExpect(jsonPath("$.unmatchedB").value(0));
    }

    @Test
    void httpIngestReportsDuplicates() throws Exception {
        String body = ev("s2", "x1", "B", "T9", "1.00", "2026-01-01");
        mvc.perform(post("/api/streams/events").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.outcome").value("STORED"));
        mvc.perform(post("/api/streams/events").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.outcome").value("DUPLICATE"));
    }

    @Test
    void invalidEventsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.ingest("{}"));
        assertThrows(IllegalArgumentException.class, () -> service.ingest(ev("s3", "e", "C", "T", "1", "2026-01-01")));
        assertThrows(IllegalArgumentException.class, () -> service.ingest(ev("s3", "e", "A", "T", "abc", "2026-01-01")));
    }

    @Test
    void reconcilingUnknownStreamIs404() throws Exception {
        mvc.perform(post("/api/streams/nope/reconcile")).andExpect(status().isNotFound());
    }
}
