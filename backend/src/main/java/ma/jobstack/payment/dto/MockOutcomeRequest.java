package ma.jobstack.payment.dto;

import jakarta.validation.constraints.NotNull;
import ma.jobstack.payment.PaymentOutcome;

public record MockOutcomeRequest(@NotNull PaymentOutcome outcome) {
}
