package ma.jobstack.job;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobPostingRepository extends JpaRepository<JobPosting, UUID> {

    Optional<JobPosting> findByIdAndStatus(UUID id, JobStatus status);

    List<JobPosting> findByStatusIn(List<JobStatus> statuses);

    long countByStatus(JobStatus status);

    List<JobPosting> findByStatusAndExpiresAtBetweenAndExpiryNoticeSentAtIsNull(
            JobStatus status, Instant from, Instant to);

    @Query("""
            SELECT p FROM JobPosting p
            WHERE p.status = :status
              AND (:sector IS NULL OR p.sector = :sector)
              AND (:city IS NULL OR p.city = :city)
              AND (:contractType IS NULL OR p.contractType = :contractType)
            """)
    List<JobPosting> search(@Param("status") JobStatus status, @Param("sector") String sector,
                             @Param("city") String city, @Param("contractType") String contractType);
}
