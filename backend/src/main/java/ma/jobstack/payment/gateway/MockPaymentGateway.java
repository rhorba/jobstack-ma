package ma.jobstack.payment.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Placeholder payment provider used until real CMI merchant credentials/docs are available
 * (see .logs/decisions.md, 2026-07-15). Signs a made-up payload shape (transactionId|outcome|amount)
 * with HMAC-SHA256 over cmi.store-key — this is NOT CMI's real signature algorithm and must be
 * replaced by a CmiPaymentGateway before any production payment goes live.
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final String storeKey;

    public MockPaymentGateway(@Value("${cmi.store-key}") String storeKey) {
        this.storeKey = storeKey;
    }

    @Override
    public CheckoutSession initiateCheckout(UUID paymentId, BigDecimal amount) {
        String transactionId = "MOCK-" + UUID.randomUUID();
        String redirectUrl = "/checkout/" + paymentId;
        return new CheckoutSession(transactionId, redirectUrl);
    }

    @Override
    public String sign(String transactionId, String outcome, BigDecimal amount) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(storeKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] raw = mac.doFinal(payload(transactionId, outcome, amount).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean verifySignature(String transactionId, String outcome, BigDecimal amount, String signature) {
        if (signature == null) {
            return false;
        }
        String expected = sign(transactionId, outcome, amount);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
    }

    private String payload(String transactionId, String outcome, BigDecimal amount) {
        return transactionId + "|" + outcome + "|" + amount.toPlainString();
    }
}
