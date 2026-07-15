package ma.jobstack.payment;

import ma.jobstack.employer.Company;
import ma.jobstack.employer.CompanyRepository;
import ma.jobstack.job.JobPosting;
import ma.jobstack.job.JobPostingRepository;
import ma.jobstack.job.JobStatus;
import ma.jobstack.payment.gateway.CheckoutSession;
import ma.jobstack.payment.gateway.PaymentGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final Duration LIVE_DURATION = Duration.ofDays(30);
    private static final BigDecimal POSTING_PRICE_MAD = new BigDecimal("490.00");

    private final JobPostingRepository jobPostingRepository;
    private final CompanyRepository companyRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;

    public PaymentService(JobPostingRepository jobPostingRepository, CompanyRepository companyRepository,
                           PaymentRepository paymentRepository, PaymentGateway paymentGateway) {
        this.jobPostingRepository = jobPostingRepository;
        this.companyRepository = companyRepository;
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
    }

    public record CheckoutResult(UUID paymentId, String transactionId, String redirectUrl, BigDecimal amount) {
    }

    @Transactional
    public CheckoutResult checkout(UUID jobPostingId, UUID employerUserId) {
        JobPosting posting = jobPostingRepository.findById(jobPostingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        assertOwnedByEmployer(posting, employerUserId);

        if (posting.getStatus() != JobStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Job posting is not in DRAFT status");
        }
        if (paymentRepository.existsByJobPostingId(jobPostingId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Checkout already started for this job posting");
        }

        Payment payment = new Payment(jobPostingId, null, POSTING_PRICE_MAD);
        paymentRepository.save(payment);

        CheckoutSession session = paymentGateway.initiateCheckout(payment.getId(), POSTING_PRICE_MAD);
        payment.setCmiTransactionId(session.transactionId());
        paymentRepository.save(payment);

        posting.setStatus(JobStatus.PENDING_PAYMENT);
        jobPostingRepository.save(posting);

        return new CheckoutResult(payment.getId(), session.transactionId(), session.redirectUrl(), POSTING_PRICE_MAD);
    }

    @Transactional
    public void mockOutcome(UUID paymentId, UUID employerUserId, PaymentOutcome outcome) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        JobPosting posting = jobPostingRepository.findById(payment.getJobPostingId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        assertOwnedByEmployer(posting, employerUserId);

        String signature = paymentGateway.sign(payment.getCmiTransactionId(), outcome.name(), payment.getAmountMad());
        handleCallback(payment.getCmiTransactionId(), outcome, payment.getAmountMad(), signature);
    }

    @Transactional
    public void handleCallback(String transactionId, PaymentOutcome outcome, BigDecimal amount, String signature) {
        if (!paymentGateway.verifySignature(transactionId, outcome.name(), amount, signature)) {
            log.warn("Rejected payment callback with invalid signature for transaction {}", transactionId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid signature");
        }

        Payment payment = paymentRepository.findByCmiTransactionId(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (payment.getStatus() == PaymentStatus.CONFIRMED) {
            log.info("Ignoring duplicate callback for already-confirmed transaction {}", transactionId);
            return;
        }

        if (amount.compareTo(payment.getAmountMad()) != 0) {
            log.warn("Rejected payment callback with amount mismatch for transaction {}: expected {} got {}",
                    transactionId, payment.getAmountMad(), amount);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount mismatch");
        }

        JobPosting posting = jobPostingRepository.findById(payment.getJobPostingId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (outcome == PaymentOutcome.SUCCESS) {
            payment.setStatus(PaymentStatus.CONFIRMED);
            payment.setConfirmedAt(Instant.now());
            posting.activate(Instant.now(), LIVE_DURATION);
            jobPostingRepository.save(posting);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }
        paymentRepository.save(payment);
    }

    private void assertOwnedByEmployer(JobPosting posting, UUID employerUserId) {
        Company company = companyRepository.findById(posting.getCompanyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!company.getOwnerUserId().equals(employerUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
}
