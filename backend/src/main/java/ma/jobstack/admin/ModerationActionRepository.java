package ma.jobstack.admin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ModerationActionRepository extends JpaRepository<ModerationAction, UUID> {
}
