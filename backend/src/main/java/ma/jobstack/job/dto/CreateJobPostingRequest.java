package ma.jobstack.job.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateJobPostingRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String description,
        @NotBlank @Size(max = 50) String sector,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 30) String contractType
) {
}
