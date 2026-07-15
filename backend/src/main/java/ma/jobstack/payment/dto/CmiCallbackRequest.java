package ma.jobstack.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ma.jobstack.payment.PaymentOutcome;

import java.math.BigDecimal;

public record CmiCallbackRequest(
        @NotBlank String transactionId,
        @NotNull PaymentOutcome outcome,
        @NotNull BigDecimal amount,
        @NotBlank String signature
) {
}
