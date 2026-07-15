package ma.jobstack.payment.dto;

import java.math.BigDecimal;

public record CheckoutResponse(String paymentId, String transactionId, String redirectUrl, BigDecimal amount) {
}
