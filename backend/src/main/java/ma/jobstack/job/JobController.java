package ma.jobstack.job;

import jakarta.validation.Valid;
import ma.jobstack.employer.Company;
import ma.jobstack.employer.CompanyRepository;
import ma.jobstack.job.dto.CreateJobPostingRequest;
import ma.jobstack.job.dto.JobPostingResponse;
import ma.jobstack.payment.PaymentService;
import ma.jobstack.payment.dto.CheckoutResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final CompanyRepository companyRepository;
    private final JobPostingRepository jobPostingRepository;
    private final PaymentService paymentService;

    public JobController(CompanyRepository companyRepository, JobPostingRepository jobPostingRepository,
                          PaymentService paymentService) {
        this.companyRepository = companyRepository;
        this.jobPostingRepository = jobPostingRepository;
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<JobPostingResponse> create(@AuthenticationPrincipal UUID userId,
                                                       @Valid @RequestBody CreateJobPostingRequest request) {
        Company company = companyRepository.findByOwnerUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Register a company before posting a job"));

        JobPosting posting = new JobPosting(company.getId(), request.title(), request.description(),
                request.sector(), request.city(), request.contractType());
        jobPostingRepository.save(posting);
        return ResponseEntity.ok(toResponse(posting, company.getName()));
    }

    @PostMapping("/{id}/checkout")
    public ResponseEntity<CheckoutResponse> checkout(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        PaymentService.CheckoutResult result = paymentService.checkout(id, userId);
        return ResponseEntity.ok(new CheckoutResponse(result.paymentId().toString(), result.transactionId(),
                result.redirectUrl(), result.amount()));
    }

    @GetMapping
    public ResponseEntity<List<JobPostingResponse>> search(@RequestParam(required = false) String sector,
                                                             @RequestParam(required = false) String city,
                                                             @RequestParam(required = false) String contractType) {
        List<JobPosting> results = jobPostingRepository.search(JobStatus.LIVE, sector, city, contractType);
        Map<UUID, String> companyNames = companyNamesFor(results);
        return ResponseEntity.ok(results.stream()
                .map(p -> toResponse(p, companyNames.get(p.getCompanyId())))
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobPostingResponse> getById(@PathVariable UUID id) {
        JobPosting posting = jobPostingRepository.findByIdAndStatus(id, JobStatus.LIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String companyName = companyRepository.findById(posting.getCompanyId()).map(Company::getName).orElse(null);
        return ResponseEntity.ok(toResponse(posting, companyName));
    }

    private Map<UUID, String> companyNamesFor(List<JobPosting> postings) {
        List<UUID> companyIds = postings.stream().map(JobPosting::getCompanyId).distinct().toList();
        return companyRepository.findAllById(companyIds).stream()
                .collect(Collectors.toMap(Company::getId, Company::getName));
    }

    private JobPostingResponse toResponse(JobPosting posting, String companyName) {
        return new JobPostingResponse(posting.getId().toString(), posting.getTitle(), posting.getDescription(),
                posting.getSector(), posting.getCity(), posting.getContractType(), posting.getStatus().name(),
                companyName);
    }
}
