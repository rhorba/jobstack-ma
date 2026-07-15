package ma.jobstack.payment;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "job_posting_id", nullable = false, unique = true)
    private UUID jobPostingId;

    @Column(name = "cmi_transaction_id", unique = true)
    private String cmiTransactionId;

    @Column(name = "amount_mad", nullable = false)
    private BigDecimal amountMad = new BigDecimal("490.00");

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status = PaymentStatus.INITIATED;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Payment() {
    }

    public Payment(UUID jobPostingId, String cmiTransactionId, BigDecimal amountMad) {
        this.jobPostingId = jobPostingId;
        this.cmiTransactionId = cmiTransactionId;
        this.amountMad = amountMad;
    }

    public UUID getId() {
        return id;
    }

    public UUID getJobPostingId() {
        return jobPostingId;
    }

    public String getCmiTransactionId() {
        return cmiTransactionId;
    }

    public void setCmiTransactionId(String cmiTransactionId) {
        this.cmiTransactionId = cmiTransactionId;
    }

    public BigDecimal getAmountMad() {
        return amountMad;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }
}
