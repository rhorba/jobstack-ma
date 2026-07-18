package ma.jobstack.job;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_postings")
public class JobPosting {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "sector", nullable = false)
    private String sector;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "contract_type", nullable = false)
    private String contractType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private JobStatus status = JobStatus.DRAFT;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "expiry_notice_sent_at")
    private Instant expiryNoticeSentAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected JobPosting() {
    }

    public JobPosting(UUID companyId, String title, String description, String sector, String city,
                       String contractType) {
        this.companyId = companyId;
        this.title = title;
        this.description = description;
        this.sector = sector;
        this.city = city;
        this.contractType = contractType;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getSector() {
        return sector;
    }

    public String getCity() {
        return city;
    }

    public String getContractType() {
        return contractType;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public Instant getExpiryNoticeSentAt() {
        return expiryNoticeSentAt;
    }

    public void markExpiryNoticeSent(Instant now) {
        this.expiryNoticeSentAt = now;
    }

    public void activate(Instant now, Duration liveDuration) {
        this.status = JobStatus.LIVE;
        this.publishedAt = now;
        this.expiresAt = now.plus(liveDuration);
    }

    public void reject(String reason) {
        this.status = JobStatus.REJECTED;
        this.rejectionReason = reason;
    }

    public void remove() {
        this.status = JobStatus.REMOVED;
    }
}
