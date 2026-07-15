package ma.jobstack.employer;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "sector", nullable = false)
    private String sector;

    @Column(name = "city")
    private String city;

    @Column(name = "verified", nullable = false)
    private boolean verified = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Company() {
    }

    public Company(UUID ownerUserId, String name, String sector, String city) {
        this.ownerUserId = ownerUserId;
        this.name = name;
        this.sector = sector;
        this.city = city;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public String getName() {
        return name;
    }

    public String getSector() {
        return sector;
    }

    public String getCity() {
        return city;
    }

    public boolean isVerified() {
        return verified;
    }
}
