package ma.jobstack.employer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCompanyRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 50) String sector,
        @Size(max = 100) String city
) {
}
