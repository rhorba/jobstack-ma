package ma.jobstack.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    boolean existsByJobPostingIdAndCandidateProfileId(UUID jobPostingId, UUID candidateProfileId);

    Page<Application> findByJobPostingId(UUID jobPostingId, Pageable pageable);
}
