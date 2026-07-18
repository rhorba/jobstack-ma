package ma.jobstack.admin;

import ma.jobstack.admin.dto.AdminMetricsResponse;
import ma.jobstack.application.ApplicationRepository;
import ma.jobstack.job.JobPostingRepository;
import ma.jobstack.job.JobStatus;
import ma.jobstack.payment.PaymentRepository;
import ma.jobstack.payment.PaymentStatus;
import org.springframework.stereotype.Service;

@Service
public class AdminMetricsService {

    private final JobPostingRepository jobPostingRepository;
    private final ApplicationRepository applicationRepository;
    private final PaymentRepository paymentRepository;

    public AdminMetricsService(JobPostingRepository jobPostingRepository, ApplicationRepository applicationRepository,
                                PaymentRepository paymentRepository) {
        this.jobPostingRepository = jobPostingRepository;
        this.applicationRepository = applicationRepository;
        this.paymentRepository = paymentRepository;
    }

    public AdminMetricsResponse metrics() {
        return new AdminMetricsResponse(
                jobPostingRepository.count(),
                jobPostingRepository.countByStatus(JobStatus.LIVE),
                applicationRepository.count(),
                paymentRepository.countByStatus(PaymentStatus.CONFIRMED),
                paymentRepository.sumAmountByStatus(PaymentStatus.CONFIRMED));
    }
}
