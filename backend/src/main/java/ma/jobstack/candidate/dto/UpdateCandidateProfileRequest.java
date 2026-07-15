package ma.jobstack.candidate.dto;

import jakarta.validation.constraints.Size;

public record UpdateCandidateProfileRequest(
        @Size(max = 200) String fullName,
        @Size(max = 30) String phone,
        @Size(max = 50) String sector,
        @Size(max = 100) String city
) {
}
