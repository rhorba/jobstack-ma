package ma.jobstack.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByJobPostingId(UUID jobPostingId);

    Optional<Payment> findByCmiTransactionId(String cmiTransactionId);

    boolean existsByJobPostingId(UUID jobPostingId);
}
