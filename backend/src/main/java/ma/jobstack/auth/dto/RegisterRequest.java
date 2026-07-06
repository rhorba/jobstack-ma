package ma.jobstack.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ma.jobstack.auth.UserRole;

public record RegisterRequest(
        @NotNull @Email String email,
        @NotNull @Size(min = 10, message = "Password must be at least 10 characters") String password,
        @NotNull UserRole role
) {
    // ADMIN is intentionally excluded from self-registration; enforced in AuthService, not here,
    // since @Pattern-style validation doesn't apply to enum-typed fields.
}
