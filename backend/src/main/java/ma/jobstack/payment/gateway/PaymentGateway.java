package ma.jobstack.payment.gateway;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Abstraction over the payment provider so a real CmiPaymentGateway can replace
 * {@link MockPaymentGateway} later without touching PaymentService's state machine.
 */
public interface PaymentGateway {

    CheckoutSession initiateCheckout(UUID paymentId, BigDecimal amount);

    String sign(String transactionId, String outcome, BigDecimal amount);

    boolean verifySignature(String transactionId, String outcome, BigDecimal amount, String signature);
}
