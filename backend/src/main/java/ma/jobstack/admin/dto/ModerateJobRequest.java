package ma.jobstack.admin.dto;

import jakarta.validation.constraints.NotNull;
import ma.jobstack.admin.ModerationActionType;

public record ModerateJobRequest(
        @NotNull ModerationActionType action,
        String reason
) {
}
