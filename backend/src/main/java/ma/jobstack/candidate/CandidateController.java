package ma.jobstack.candidate;

import jakarta.validation.Valid;
import ma.jobstack.auth.User;
import ma.jobstack.auth.UserRepository;
import ma.jobstack.candidate.dto.CandidateProfileResponse;
import ma.jobstack.candidate.dto.UpdateCandidateProfileRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/candidates")
public class CandidateController {

    private final UserRepository userRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final CvStorageService cvStorageService;

    public CandidateController(UserRepository userRepository, CandidateProfileRepository candidateProfileRepository,
                                CvStorageService cvStorageService) {
        this.userRepository = userRepository;
        this.candidateProfileRepository = candidateProfileRepository;
        this.cvStorageService = cvStorageService;
    }

    @GetMapping("/me")
    public ResponseEntity<CandidateProfileResponse> me(@AuthenticationPrincipal UUID userId) {
        User user = findUser(userId);
        CandidateProfile profile = findProfile(userId);
        return ResponseEntity.ok(toResponse(user, profile));
    }

    @PutMapping("/me")
    public ResponseEntity<CandidateProfileResponse> updateMe(@AuthenticationPrincipal UUID userId,
                                                              @Valid @RequestBody UpdateCandidateProfileRequest request) {
        User user = findUser(userId);
        CandidateProfile profile = findProfile(userId);
        profile.setFullName(request.fullName());
        profile.setPhone(request.phone());
        profile.setSector(request.sector());
        profile.setCity(request.city());
        candidateProfileRepository.save(profile);
        return ResponseEntity.ok(toResponse(user, profile));
    }

    @PostMapping("/me/cv")
    public ResponseEntity<CandidateProfileResponse> uploadCv(@AuthenticationPrincipal UUID userId,
                                                               @RequestParam("file") MultipartFile file) {
        User user = findUser(userId);
        CandidateProfile profile = findProfile(userId);
        String cvPath = cvStorageService.store(userId, file);
        profile.setCvPath(cvPath);
        candidateProfileRepository.save(profile);
        return ResponseEntity.ok(toResponse(user, profile));
    }

    @GetMapping("/me/cv")
    public ResponseEntity<byte[]> downloadCv(@AuthenticationPrincipal UUID userId) {
        byte[] content = cvStorageService.load(userId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(content);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private CandidateProfile findProfile(UUID userId) {
        return candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private CandidateProfileResponse toResponse(User user, CandidateProfile profile) {
        return new CandidateProfileResponse(
                user.getEmail(),
                user.getRole().name(),
                profile.getFullName(),
                profile.getPhone(),
                profile.getSector(),
                profile.getCity(),
                profile.getCvPath() != null);
    }
}
