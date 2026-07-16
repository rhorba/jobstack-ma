package ma.jobstack.application;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    boolean existsByJobPostingIdAndCandidateProfileId(UUID jobPostingId, UUID candidateProfileId);

    List<Application> findByJobPostingId(UUID jobPostingId);
}
