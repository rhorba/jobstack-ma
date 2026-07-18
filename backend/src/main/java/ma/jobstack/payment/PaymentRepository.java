package ma.jobstack.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByJobPostingId(UUID jobPostingId);

    Optional<Payment> findByCmiTransactionId(String cmiTransactionId);

    boolean existsByJobPostingId(UUID jobPostingId);

    long countByStatus(PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amountMad), 0) FROM Payment p WHERE p.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") PaymentStatus status);
}
