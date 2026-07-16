package ma.jobstack.application;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "job_posting_id", nullable = false)
    private UUID jobPostingId;

    @Column(name = "candidate_profile_id", nullable = false)
    private UUID candidateProfileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ApplicationStatus status = ApplicationStatus.SUBMITTED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Application() {
    }

    public Application(UUID jobPostingId, UUID candidateProfileId) {
        this.jobPostingId = jobPostingId;
        this.candidateProfileId = candidateProfileId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getJobPostingId() {
        return jobPostingId;
    }

    public UUID getCandidateProfileId() {
        return candidateProfileId;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
