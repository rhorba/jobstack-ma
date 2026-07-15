package ma.jobstack.payment.gateway;

public record CheckoutSession(String transactionId, String redirectUrl) {
}
