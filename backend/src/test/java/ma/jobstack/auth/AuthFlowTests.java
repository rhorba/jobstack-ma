package ma.jobstack.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import ma.jobstack.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@jobstack.ma";
    }

    private String registerAndLogin(String email, String role) throws Exception {
        String password = "supersecret123";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password, "role", role))))
                .andExpect(status().isOk());
        return password;
    }

    private MvcResult login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
    }

    private String extractCookieValue(MvcResult result, String cookieName) {
        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertThat(setCookie).isNotNull();
        Matcher matcher = Pattern.compile(cookieName + "=([^;]+)").matcher(setCookie);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private String accessToken(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    @Test
    void registerThenLogin_succeeds() throws Exception {
        String email = uniqueEmail();
        String password = registerAndLogin(email, "CANDIDATE");
        MvcResult result = login(email, password);

        assertThat(accessToken(result)).isNotBlank();
        assertThat(extractCookieValue(result, "refresh_token")).isNotBlank();
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        String email = uniqueEmail();
        registerAndLogin(email, "CANDIDATE");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "supersecret123", "role", "CANDIDATE"))))
                .andExpect(status().isConflict());
    }

    @Test
    void register_asAdmin_isRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", uniqueEmail(), "password", "supersecret123", "role", "ADMIN"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        String email = uniqueEmail();
        registerAndLogin(email, "CANDIDATE");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "wrongpassword"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_unknownEmail_returns401_sameAsWrongPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", uniqueEmail(), "password", "whatever123"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_withValidCookie_rotatesTokenAndOldOneIsRejected() throws Exception {
        String email = uniqueEmail();
        String password = registerAndLogin(email, "CANDIDATE");
        MvcResult loginResult = login(email, password);
        String oldRefreshToken = extractCookieValue(loginResult, "refresh_token");

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", oldRefreshToken)))
                .andExpect(status().isOk())
                .andReturn();
        String newRefreshToken = extractCookieValue(refreshResult, "refresh_token");
        assertThat(newRefreshToken).isNotEqualTo(oldRefreshToken);

        // Reusing the rotated-out token must be rejected (adversarial check, test-strategy §4)
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", oldRefreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_withoutCookie_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_thenRefresh_isRejected() throws Exception {
        String email = uniqueEmail();
        String password = registerAndLogin(email, "CANDIDATE");
        MvcResult loginResult = login(email, password);
        String refreshToken = extractCookieValue(loginResult, "refresh_token");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", refreshToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void candidateToken_canAccessOwnEndpoint_butNotEmployerEndpoint() throws Exception {
        String email = uniqueEmail();
        String password = registerAndLogin(email, "CANDIDATE");
        String token = accessToken(login(email, password));

        mockMvc.perform(get("/api/v1/candidates/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CANDIDATE"));

        mockMvc.perform(get("/api/v1/employers/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void noToken_isRejectedFromProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/candidates/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void tamperedToken_isRejected() throws Exception {
        String email = uniqueEmail();
        String password = registerAndLogin(email, "CANDIDATE");
        String token = accessToken(login(email, password));

        mockMvc.perform(get("/api/v1/candidates/me").header("Authorization", "Bearer " + token + "tampered"))
                .andExpect(status().isForbidden());
    }

    @Test
    void expiredToken_isRejected() throws Exception {
        SecretKey signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        String expiredToken = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("email", uniqueEmail())
                .claim("role", "CANDIDATE")
                .issuedAt(Date.from(now.minus(1, ChronoUnit.HOURS)))
                .expiration(Date.from(now.minus(1, ChronoUnit.MINUTES)))
                .signWith(signingKey)
                .compact();

        mockMvc.perform(get("/api/v1/candidates/me").header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void roleClaimTampering_isRejected() throws Exception {
        String email = uniqueEmail();
        String password = registerAndLogin(email, "CANDIDATE");
        String token = accessToken(login(email, password));

        // Attacker edits the "role" claim in the (unsigned-by-them) payload segment without the signing key,
        // then re-attaches the original signature — signature verification must catch the mismatch.
        String[] parts = token.split("\\.");
        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        String tamperedPayloadJson = payloadJson.replace("\"role\":\"CANDIDATE\"", "\"role\":\"ADMIN\"");
        assertThat(tamperedPayloadJson).isNotEqualTo(payloadJson); // guard: replacement actually happened
        String tamperedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(tamperedPayloadJson.getBytes(StandardCharsets.UTF_8));
        String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

        mockMvc.perform(get("/api/v1/employers/me").header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isForbidden());
    }
}
