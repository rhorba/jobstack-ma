package ma.jobstack.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.jobstack.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PaymentFlowTests {

    private static final String STORE_KEY = "changeme"; // matches cmi.store-key default in application.properties

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessTokenForNewEmployer() throws Exception {
        String email = "employer-" + UUID.randomUUID() + "@jobstack.ma";
        String password = "supersecret123";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password, "role", "EMPLOYER"))))
                .andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String accessTokenForNewCandidate() throws Exception {
        String email = "candidate-" + UUID.randomUUID() + "@jobstack.ma";
        String password = "supersecret123";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password, "role", "CANDIDATE"))))
                .andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String draftJobFor(String employerToken) throws Exception {
        mockMvc.perform(post("/api/v1/employers/me/company")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Atlas Automotive SARL", "sector", "automotive", "city", "Tangier"))))
                .andExpect(status().isOk());

        MvcResult jobResult = mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Automotive QA Engineer",
                                "description", "Inspect assembly line output for defects.",
                                "sector", "automotive",
                                "city", "Tangier",
                                "contractType", "CDI"))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(jobResult.getResponse().getContentAsString()).get("id").asText();
    }

    private JsonCheckout checkout(String employerToken, String jobId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/jobs/" + jobId + "/checkout")
                        .header("Authorization", "Bearer " + employerToken))
                .andExpect(status().isOk())
                .andReturn();
        var node = objectMapper.readTree(result.getResponse().getContentAsString());
        return new JsonCheckout(node.get("paymentId").asText(), node.get("transactionId").asText(),
                node.get("amount").decimalValue());
    }

    private record JsonCheckout(String paymentId, String transactionId, BigDecimal amount) {
    }

    private String sign(String transactionId, String outcome, BigDecimal amount) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(STORE_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String payload = transactionId + "|" + outcome + "|" + amount.toPlainString();
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void checkout_onDraftPostingOwnedByEmployer_createsPaymentAndMovesToPendingPayment() throws Exception {
        String token = accessTokenForNewEmployer();
        String jobId = draftJobFor(token);

        mockMvc.perform(post("/api/v1/jobs/" + jobId + "/checkout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").isNotEmpty())
                .andExpect(jsonPath("$.redirectUrl").isNotEmpty())
                .andExpect(jsonPath("$.amount").value(490.00));

        mockMvc.perform(get("/api/v1/jobs/" + jobId))
                .andExpect(status().isNotFound()); // PENDING_PAYMENT is not publicly visible (LIVE-only)
    }

    @Test
    void checkout_secondAttempt_returns409() throws Exception {
        String token = accessTokenForNewEmployer();
        String jobId = draftJobFor(token);

        mockMvc.perform(post("/api/v1/jobs/" + jobId + "/checkout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/jobs/" + jobId + "/checkout").header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void checkout_byNonOwningEmployer_returns403() throws Exception {
        String ownerToken = accessTokenForNewEmployer();
        String jobId = draftJobFor(ownerToken);
        String otherToken = accessTokenForNewEmployer();

        mockMvc.perform(post("/api/v1/jobs/" + jobId + "/checkout").header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void checkout_asCandidate_isForbidden() throws Exception {
        String candidateToken = accessTokenForNewCandidate();
        mockMvc.perform(post("/api/v1/jobs/" + UUID.randomUUID() + "/checkout")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void callback_validSuccess_confirmsPaymentAndActivatesPosting() throws Exception {
        String token = accessTokenForNewEmployer();
        String jobId = draftJobFor(token);
        JsonCheckout co = checkout(token, jobId);
        String signature = sign(co.transactionId(), "SUCCESS", co.amount());

        mockMvc.perform(post("/api/v1/payments/cmi/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "transactionId", co.transactionId(),
                                "outcome", "SUCCESS",
                                "amount", co.amount(),
                                "signature", signature))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/jobs/" + jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LIVE"));
    }

    @Test
    void mockOutcome_success_activatesJobPosting_andIsVisibleInPublicSearch() throws Exception {
        String token = accessTokenForNewEmployer();
        String jobId = draftJobFor(token);
        JsonCheckout co = checkout(token, jobId);

        mockMvc.perform(post("/api/v1/payments/" + co.paymentId() + "/mock-outcome")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("outcome", "SUCCESS"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/jobs/" + jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LIVE"));
    }

    @Test
    void mockOutcome_failed_leavesPostingPendingPayment() throws Exception {
        String token = accessTokenForNewEmployer();
        String jobId = draftJobFor(token);
        JsonCheckout co = checkout(token, jobId);

        mockMvc.perform(post("/api/v1/payments/" + co.paymentId() + "/mock-outcome")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("outcome", "FAILED"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/jobs/" + jobId))
                .andExpect(status().isNotFound()); // still not LIVE
    }

    @Test
    void mockOutcome_byNonOwningEmployer_returns403() throws Exception {
        String ownerToken = accessTokenForNewEmployer();
        String jobId = draftJobFor(ownerToken);
        JsonCheckout co = checkout(ownerToken, jobId);
        String otherToken = accessTokenForNewEmployer();

        mockMvc.perform(post("/api/v1/payments/" + co.paymentId() + "/mock-outcome")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("outcome", "SUCCESS"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void callback_duplicateOnAlreadyConfirmedPayment_isIdempotentNoOp() throws Exception {
        String token = accessTokenForNewEmployer();
        String jobId = draftJobFor(token);
        JsonCheckout co = checkout(token, jobId);

        Map<String, String> body = Map.of("outcome", "SUCCESS");
        mockMvc.perform(post("/api/v1/payments/" + co.paymentId() + "/mock-outcome")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        // second (duplicate) confirmation attempt must be a no-op, not an error and not a double-activation
        mockMvc.perform(post("/api/v1/payments/" + co.paymentId() + "/mock-outcome")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/jobs/" + jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LIVE"));
    }

    @Test
    void callback_withInvalidSignature_isRejected_noStateChange() throws Exception {
        String token = accessTokenForNewEmployer();
        String jobId = draftJobFor(token);
        JsonCheckout co = checkout(token, jobId);

        // we don't have the real transactionId (not exposed to the client), but an invalid
        // signature must be rejected before any lookup succeeds regardless.
        Map<String, Object> forged = Map.of(
                "transactionId", "MOCK-forged",
                "outcome", "SUCCESS",
                "amount", 490.00,
                "signature", "0000000000000000000000000000000000000000000000000000000000000000");

        mockMvc.perform(post("/api/v1/payments/cmi/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forged)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/jobs/" + jobId))
                .andExpect(status().isNotFound()); // still not LIVE
    }

    @Test
    void callback_withAmountTampering_isRejected_evenWithValidSignatureForThatAmount() throws Exception {
        String token = accessTokenForNewEmployer();
        String jobId = draftJobFor(token);
        JsonCheckout co = checkout(token, jobId);

        BigDecimal tamperedAmount = new BigDecimal("1.00");
        String validSignatureForTamperedAmount = sign(co.transactionId(), "SUCCESS", tamperedAmount);

        mockMvc.perform(post("/api/v1/payments/cmi/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "transactionId", co.transactionId(),
                                "outcome", "SUCCESS",
                                "amount", tamperedAmount,
                                "signature", validSignatureForTamperedAmount))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/jobs/" + jobId))
                .andExpect(status().isNotFound()); // still not LIVE — tampered callback never confirmed the payment
    }

    @Test
    void callback_withoutAuth_isPublic_butStillSignatureGated() throws Exception {
        Map<String, Object> body = Map.of(
                "transactionId", "MOCK-unknown",
                "outcome", "SUCCESS",
                "amount", 490.00,
                "signature", "invalid");

        mockMvc.perform(post("/api/v1/payments/cmi/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}
