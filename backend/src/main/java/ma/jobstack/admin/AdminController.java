package ma.jobstack.admin;

import jakarta.validation.Valid;
import ma.jobstack.admin.dto.AdminMetricsResponse;
import ma.jobstack.admin.dto.ModerateJobRequest;
import ma.jobstack.admin.dto.UpdateUserStatusRequest;
import ma.jobstack.admin.dto.UserStatusResponse;
import ma.jobstack.auth.User;
import ma.jobstack.employer.Company;
import ma.jobstack.employer.CompanyRepository;
import ma.jobstack.job.JobPosting;
import ma.jobstack.job.dto.JobPostingResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminModerationService adminModerationService;
    private final AdminUserService adminUserService;
    private final AdminMetricsService adminMetricsService;
    private final CompanyRepository companyRepository;

    public AdminController(AdminModerationService adminModerationService, AdminUserService adminUserService,
                            AdminMetricsService adminMetricsService, CompanyRepository companyRepository) {
        this.adminModerationService = adminModerationService;
        this.adminUserService = adminUserService;
        this.adminMetricsService = adminMetricsService;
        this.companyRepository = companyRepository;
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("ok");
    }

    @GetMapping("/postings")
    public ResponseEntity<List<JobPostingResponse>> moderationQueue() {
        List<JobPosting> postings = adminModerationService.listQueue();
        Map<UUID, String> companyNames = companyRepository.findAllById(
                        postings.stream().map(JobPosting::getCompanyId).distinct().toList()).stream()
                .collect(Collectors.toMap(Company::getId, Company::getName));
        return ResponseEntity.ok(postings.stream()
                .map(p -> toResponse(p, companyNames.get(p.getCompanyId())))
                .toList());
    }

    @PostMapping("/postings/{id}/moderate")
    public ResponseEntity<JobPostingResponse> moderate(@AuthenticationPrincipal UUID adminUserId,
                                                         @PathVariable UUID id,
                                                         @Valid @RequestBody ModerateJobRequest request) {
        JobPosting posting = adminModerationService.moderate(id, adminUserId, request.action(), request.reason());
        String companyName = companyRepository.findById(posting.getCompanyId()).map(Company::getName).orElse(null);
        return ResponseEntity.ok(toResponse(posting, companyName));
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<UserStatusResponse> setUserStatus(@PathVariable UUID id,
                                                              @Valid @RequestBody UpdateUserStatusRequest request) {
        User user = adminUserService.setStatus(id, request.status());
        return ResponseEntity.ok(new UserStatusResponse(user.getId().toString(), user.getEmail(), user.getStatus().name()));
    }

    @GetMapping("/metrics")
    public ResponseEntity<AdminMetricsResponse> metrics() {
        return ResponseEntity.ok(adminMetricsService.metrics());
    }

    private JobPostingResponse toResponse(JobPosting posting, String companyName) {
        return new JobPostingResponse(posting.getId().toString(), posting.getTitle(), posting.getDescription(),
                posting.getSector(), posting.getCity(), posting.getContractType(), posting.getStatus().name(),
                companyName);
    }
}
