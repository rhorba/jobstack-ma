package ma.jobstack.analytics;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AnalyticsServiceTests {

    @Test
    @SuppressWarnings("unchecked")
    void track_whenEnabled_postsEventToPostHog() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("1"));
        AnalyticsService service = new AnalyticsService(restTemplate, "phc_test_key", "https://app.posthog.com");
        UUID userId = UUID.randomUUID();

        service.track("registration", userId, Map.of("role", "CANDIDATE"));

        var urlCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        var bodyCaptor = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(restTemplate).postForEntity(urlCaptor.capture(), bodyCaptor.capture(), eq(String.class));
        assertThat(urlCaptor.getValue()).isEqualTo("https://app.posthog.com/capture/");
        Map<String, Object> body = bodyCaptor.getValue();
        assertThat(body.get("api_key")).isEqualTo("phc_test_key");
        assertThat(body.get("event")).isEqualTo("registration");
        assertThat(body.get("distinct_id")).isEqualTo(userId.toString());
    }

    @Test
    void track_whenApiKeyBlank_doesNotCallPostHog() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        AnalyticsService service = new AnalyticsService(restTemplate, "", "https://app.posthog.com");

        service.track("registration", UUID.randomUUID(), Map.of());

        verifyNoInteractions(restTemplate);
    }

    @Test
    void track_whenApiKeyDisabled_doesNotCallPostHog() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        AnalyticsService service = new AnalyticsService(restTemplate, "disabled", "https://app.posthog.com");

        service.track("registration", UUID.randomUUID(), Map.of());

        verifyNoInteractions(restTemplate);
    }

    @Test
    void track_whenPostHogUnreachable_logsInsteadOfThrowing() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenThrow(new RestClientException("connection refused"));
        AnalyticsService service = new AnalyticsService(restTemplate, "phc_test_key", "https://app.posthog.com");

        assertThatCode(() -> service.track("registration", UUID.randomUUID(), Map.of()))
                .doesNotThrowAnyException();
    }
}
