package ma.jobstack.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Fires PostHog capture events for key funnel steps. No-ops when POSTHOG_API_KEY is
 * unset/blank/"disabled" so the app runs cleanly without a PostHog account (see decisions.md, Sprint 8).
 */
@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String captureUrl;
    private final boolean enabled;

    public AnalyticsService(RestTemplate restTemplate,
                             @Value("${posthog.api-key:}") String apiKey,
                             @Value("${posthog.host:https://app.posthog.com}") String host) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.captureUrl = host + "/capture/";
        this.enabled = apiKey != null && !apiKey.isBlank() && !"disabled".equalsIgnoreCase(apiKey);
    }

    @Async
    public void track(String event, UUID distinctId, Map<String, Object> properties) {
        if (!enabled) {
            return;
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("api_key", apiKey);
            body.put("event", event);
            body.put("distinct_id", distinctId.toString());
            body.put("properties", properties == null ? Map.of() : properties);
            body.put("timestamp", Instant.now().toString());

            restTemplate.postForEntity(captureUrl, body, String.class);
        } catch (RestClientException e) {
            log.warn("Failed to send PostHog event '{}': {}", event, e.getMessage());
        }
    }
}
