package dev.manas.recon.stream;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** A transaction received from the event stream, staged until a reconcile is requested. */
@Entity
@Table(name = "txn_event")
public class TxnEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "stream_id", nullable = false)
    private String streamId;
    @Column(name = "event_id", nullable = false)
    private String eventId;
    @Column(nullable = false)
    private String side;
    @Column(nullable = false)
    private String reference;
    @Column(nullable = false)
    private BigDecimal amount;
    @Column(nullable = false)
    private String currency;
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;
    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt = LocalDateTime.now();

    protected TxnEvent() {}

    public TxnEvent(String streamId, String eventId, String side, String reference, BigDecimal amount, String currency, LocalDate tradeDate) {
        this.streamId = streamId;
        this.eventId = eventId;
        this.side = side;
        this.reference = reference;
        this.amount = amount;
        this.currency = currency;
        this.tradeDate = tradeDate;
    }

    public Long getId() { return id; }
    public String getStreamId() { return streamId; }
    public String getEventId() { return eventId; }
    public String getSide() { return side; }
    public String getReference() { return reference; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public LocalDate getTradeDate() { return tradeDate; }
}
