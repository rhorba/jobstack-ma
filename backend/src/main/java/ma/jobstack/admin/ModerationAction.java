package ma.jobstack.admin;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "moderation_actions")
public class ModerationAction {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "job_posting_id", nullable = false)
    private UUID jobPostingId;

    @Column(name = "admin_user_id", nullable = false)
    private UUID adminUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private ModerationActionType action;

    @Column(name = "reason")
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ModerationAction() {
    }

    public ModerationAction(UUID jobPostingId, UUID adminUserId, ModerationActionType action, String reason) {
        this.jobPostingId = jobPostingId;
        this.adminUserId = adminUserId;
        this.action = action;
        this.reason = reason;
    }

    public UUID getId() {
        return id;
    }

    public UUID getJobPostingId() {
        return jobPostingId;
    }

    public UUID getAdminUserId() {
        return adminUserId;
    }

    public ModerationActionType getAction() {
        return action;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
