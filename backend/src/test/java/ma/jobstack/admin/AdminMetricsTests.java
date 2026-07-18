package ma.jobstack.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.jobstack.TestcontainersConfiguration;
import ma.jobstack.auth.User;
import ma.jobstack.auth.UserRepository;
import ma.jobstack.auth.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AdminMetricsTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    private void liveJobFor(String employerToken) throws Exception {
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
    }

    @Test
    void metrics_reflectsPostingsAndConfirmedPayments() throws Exception {
        liveJobFor(accessTokenForNewUser("EMPLOYER"));
        String adminToken = accessTokenForNewAdmin();

        mockMvc.perform(get("/api/v1/admin/metrics").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPostings").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.liveJobPostings").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.confirmedPayments").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.confirmedRevenueMad").value(org.hamcrest.Matchers.greaterThanOrEqualTo(490.0)));
    }

    @Test
    void metrics_asNonAdmin_isForbidden() throws Exception {
        String employerToken = accessTokenForNewUser("EMPLOYER");
        mockMvc.perform(get("/api/v1/admin/metrics").header("Authorization", "Bearer " + employerToken))
                .andExpect(status().isForbidden());
    }
}
