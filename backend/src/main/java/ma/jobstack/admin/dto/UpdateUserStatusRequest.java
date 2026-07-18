package ma.jobstack.admin.dto;

import jakarta.validation.constraints.NotNull;
import ma.jobstack.auth.UserStatus;

public record UpdateUserStatusRequest(
        @NotNull UserStatus status
) {
}
