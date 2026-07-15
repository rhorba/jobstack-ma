package ma.jobstack.employer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    Optional<Company> findByOwnerUserId(UUID ownerUserId);

    boolean existsByOwnerUserId(UUID ownerUserId);
}
