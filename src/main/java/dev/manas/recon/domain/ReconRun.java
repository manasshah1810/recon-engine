package dev.manas.recon.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "recon_run")
public class ReconRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "amount_tolerance", nullable = false)
    private BigDecimal amountTolerance;
    @Column(name = "date_tolerance_days", nullable = false)
    private int dateToleranceDays;
    @Column(name = "total_a", nullable = false)
    private int totalA;
    @Column(name = "total_b", nullable = false)
    private int totalB;
    @Column(name = "exact_matches", nullable = false)
    private int exactMatches;
    @Column(name = "tolerance_matches", nullable = false)
    private int toleranceMatches;
    @Column(name = "unmatched_a", nullable = false)
    private int unmatchedA;
    @Column(name = "unmatched_b", nullable = false)
    private int unmatchedB;

    protected ReconRun() {}

    public ReconRun(BigDecimal amountTolerance, int dateToleranceDays, int totalA, int totalB,
                    int exactMatches, int toleranceMatches, int unmatchedA, int unmatchedB) {
        this.createdAt = LocalDateTime.now();
        this.amountTolerance = amountTolerance;
        this.dateToleranceDays = dateToleranceDays;
        this.totalA = totalA;
        this.totalB = totalB;
        this.exactMatches = exactMatches;
        this.toleranceMatches = toleranceMatches;
        this.unmatchedA = unmatchedA;
        this.unmatchedB = unmatchedB;
    }

    public Long getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public BigDecimal getAmountTolerance() { return amountTolerance; }
    public int getDateToleranceDays() { return dateToleranceDays; }
    public int getTotalA() { return totalA; }
    public int getTotalB() { return totalB; }
    public int getExactMatches() { return exactMatches; }
    public int getToleranceMatches() { return toleranceMatches; }
    public int getUnmatchedA() { return unmatchedA; }
    public int getUnmatchedB() { return unmatchedB; }
}
