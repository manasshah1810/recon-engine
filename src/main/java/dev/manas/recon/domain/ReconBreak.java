package dev.manas.recon.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/** An item needing review: an unmatched line from either side, or a tolerance-only match. */
@Entity
@Table(name = "recon_break")
public class ReconBreak {
    public enum Type { UNMATCHED_A, UNMATCHED_B, TOLERANCE_MATCH }

    public enum Status { OPEN, RESOLVED, ESCALATED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "run_id", nullable = false)
    private Long runId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.OPEN;
    private String side;
    @Column(nullable = false)
    private String reference;
    @Column(nullable = false)
    private BigDecimal amount;
    @Column(nullable = false)
    private String currency;
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;
    @Column(name = "counter_reference")
    private String counterReference;
    @Column(name = "counter_amount")
    private BigDecimal counterAmount;
    @Column(name = "counter_date")
    private LocalDate counterDate;
    private String note;

    protected ReconBreak() {}

    public ReconBreak(Long runId, Type type, String side, String reference, BigDecimal amount, String currency, LocalDate tradeDate) {
        this.runId = runId;
        this.type = type;
        this.side = side;
        this.reference = reference;
        this.amount = amount;
        this.currency = currency;
        this.tradeDate = tradeDate;
    }

    public void setCounter(String ref, BigDecimal amt, LocalDate date) {
        this.counterReference = ref;
        this.counterAmount = amt;
        this.counterDate = date;
    }

    public void resolve(Status status, String note) {
        this.status = status;
        this.note = note;
    }

    public Long getId() { return id; }
    public Long getRunId() { return runId; }
    public Type getType() { return type; }
    public Status getStatus() { return status; }
    public String getSide() { return side; }
    public String getReference() { return reference; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public LocalDate getTradeDate() { return tradeDate; }
    public String getCounterReference() { return counterReference; }
    public BigDecimal getCounterAmount() { return counterAmount; }
    public LocalDate getCounterDate() { return counterDate; }
    public String getNote() { return note; }
}
