package ma.jobstack.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.jobstack.TestcontainersConfiguration;
import ma.jobstack.auth.User;
import ma.jobstack.auth.UserRepository;
import ma.jobstack.auth.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class NotificationTriggerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JobExpiryNotifier jobExpiryNotifier;

    @MockBean
    private EmailService emailService;

    private String accessTokenForNewUser(String role) throws Exception {
        String email = role.toLowerCase() + "-" + UUID.randomUUID() + "@jobstack.ma";
        String password = "supersecret123";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password, "role", role))))
                .andExpect(status().isOk());
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String accessTokenForNewAdmin() throws Exception {
        String email = "admin-" + UUID.randomUUID() + "@jobstack.ma";
        String password = "supersecret123";
        userRepository.save(new User(email, passwordEncoder.encode(password), UserRole.ADMIN));
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private void completeCandidateProfile(String candidateToken) throws Exception {
        mockMvc.perform(put("/api/v1/candidates/me")
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Amine Test", "phone", "0600000000", "sector", "IT", "city", "Rabat"))))
                .andExpect(status().isOk());
        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                "file", "cv.pdf", "application/pdf", "%PDF-1.4 fake".getBytes());
        mockMvc.perform(multipart("/api/v1/candidates/me/cv").file(file)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk());
    }

    private String liveJobFor(String employerToken) throws Exception {
        mockMvc.perform(post("/api/v1/employers/me/company")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Atlas Automotive SARL " + UUID.randomUUID(), "sector", "automotive", "city", "Tangier"))))
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
        String jobId = objectMapper.readTree(jobResult.getResponse().getContentAsString()).get("id").asText();
        MvcResult checkoutResult = mockMvc.perform(post("/api/v1/jobs/" + jobId + "/checkout")
                        .header("Authorization", "Bearer " + employerToken))
                .andExpect(status().isOk())
                .andReturn();
        String paymentId = objectMapper.readTree(checkoutResult.getResponse().getContentAsString())
                .get("paymentId").asText();
        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/mock-outcome")
                        .header("Authorization", "Bearer " + employerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("outcome", "SUCCESS"))))
                .andExpect(status().isOk());
        return jobId;
    }

    @Test
    void register_sendsWelcomeEmail() throws Exception {
        String email = "welcome-" + UUID.randomUUID() + "@jobstack.ma";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "supersecret123", "role", "CANDIDATE"))))
                .andExpect(status().isOk());

        verify(emailService, timeout(3000)).sendAsync(eq(email), contains("Welcome"), anyString());
    }

    @Test
    void apply_sendsApplicationSubmittedEmail() throws Exception {
        String employerToken = accessTokenForNewUser("EMPLOYER");
        String jobId = liveJobFor(employerToken);
        String candidateToken = accessTokenForNewUser("CANDIDATE");
        completeCandidateProfile(candidateToken);

        reset(emailService); // ignore the welcome emails from registration above
        mockMvc.perform(post("/api/v1/jobs/" + jobId + "/apply").header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk());

        verify(emailService, timeout(3000)).sendAsync(anyString(), contains("Application submitted"), contains("Automotive QA Engineer"));
    }

    @Test
    void rejectPosting_sendsRejectionEmail() throws Exception {
        String employerToken = accessTokenForNewUser("EMPLOYER");
        String jobId = liveJobFor(employerToken);
        String adminToken = accessTokenForNewAdmin();

        reset(emailService);
        mockMvc.perform(post("/api/v1/admin/postings/" + jobId + "/moderate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("action", "REJECT", "reason", "Misleading title"))))
                .andExpect(status().isOk());

        verify(emailService, timeout(3000)).sendAsync(anyString(), contains("rejected"), contains("Misleading title"));
    }

    @Test
    void expiryNotifier_notifiesOnceThenDoesNotResend() throws Exception {
        String employerToken = accessTokenForNewUser("EMPLOYER");
        String jobId = liveJobFor(employerToken);
        jdbcTemplate.update("UPDATE job_postings SET expires_at = ? WHERE id = ?",
                java.sql.Timestamp.from(Instant.now().plus(1, ChronoUnit.DAYS)), UUID.fromString(jobId));

        reset(emailService);
        jobExpiryNotifier.notifyExpiringSoon();
        verify(emailService, timeout(3000)).sendAsync(anyString(), contains("expires soon"), contains("Automotive QA Engineer"));

        reset(emailService);
        jobExpiryNotifier.notifyExpiringSoon();
        Thread.sleep(500);
        verifyNoInteractions(emailService);
    }
}
