package ma.jobstack.payment;

import jakarta.validation.Valid;
import ma.jobstack.payment.dto.CmiCallbackRequest;
import ma.jobstack.payment.dto.MockOutcomeRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/cmi/callback")
    public ResponseEntity<Void> callback(@Valid @RequestBody CmiCallbackRequest request) {
        paymentService.handleCallback(request.transactionId(), request.outcome(), request.amount(), request.signature());
        return ResponseEntity.ok().build();
    }

    /**
     * Dev/demo-only: simulates the CMI hosted page redirecting back with a signed result.
     * Reuses the exact same {@link PaymentService#handleCallback} path as the real callback.
     * Remove this once a real CmiPaymentGateway replaces MockPaymentGateway.
     */
    @PostMapping("/{id}/mock-outcome")
    public ResponseEntity<Void> mockOutcome(@AuthenticationPrincipal UUID userId, @PathVariable UUID id,
                                             @Valid @RequestBody MockOutcomeRequest request) {
        paymentService.mockOutcome(id, userId, request.outcome());
        return ResponseEntity.ok().build();
    }
}
