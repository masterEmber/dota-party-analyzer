package com.dimas.dotaanalyzer.service;

import com.dimas.dotaanalyzer.config.CacheConfig;
import com.dimas.dotaanalyzer.model.ApiHealthStatus;
import com.dimas.dotaanalyzer.model.HeroInfo;
import com.dimas.dotaanalyzer.model.PeerStat;
import com.dimas.dotaanalyzer.model.PlayerProfile;
import com.dimas.dotaanalyzer.model.RecentMatch;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class OpenDotaClient {

    private static final Logger log = LoggerFactory.getLogger(OpenDotaClient.class);
    private static final long RETRY_BACKOFF_MS = 750L;

    private final RestClient restClient;
    private final String baseUrl;
    private final String apiKey;
    private final int maxRetries;

    public OpenDotaClient(
            RestClient openDotaRestClient,
            @Value("${opendota.api.base-url:https://api.opendota.com/api}") String baseUrl,
            @Value("${opendota.api.api-key:}") String apiKey,
            @Value("${opendota.api.max-retries:1}") int maxRetries) {
        this.restClient = openDotaRestClient;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.maxRetries = Math.max(0, maxRetries);
    }

    @Cacheable(value = CacheConfig.HEROES_CACHE, key = "'all'")
    public Map<Integer, HeroInfo> fetchHeroes() {
        log.info("Fetching heroes constants from OpenDota API");
        return executeWithRetry("mengambil daftar hero dari OpenDota", () -> {
            Map<String, HeroInfo> response = restClient.get()
                    .uri(uriBuilder -> appendApiKey(uriBuilder.path("/constants/heroes")).build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || response.isEmpty()) {
                return Collections.emptyMap();
            }
            return response.values().stream()
                    .collect(Collectors.toMap(HeroInfo::id, Function.identity(), (a, b) -> a));
        });
    }

    @Cacheable(value = CacheConfig.PLAYER_PROFILE_CACHE, key = "#accountId")
    public PlayerProfile fetchPlayerProfile(long accountId) {
        log.info("Fetching player profile for accountId={}", accountId);
        return executeWithRetry("mengambil profil pemain", () ->
                restClient.get()
                        .uri(uriBuilder -> appendApiKey(uriBuilder.path("/players/{id}")).build(accountId))
                        .retrieve()
                        .body(PlayerProfile.class)
        );
    }

    @Cacheable(value = CacheConfig.RECENT_MATCHES_CACHE, key = "#accountId")
    public List<RecentMatch> fetchRecentMatches(long accountId) {
        log.info("Fetching recent matches for accountId={}", accountId);
        return executeWithRetry("mengambil riwayat match terakhir", () -> {
            List<RecentMatch> matches = restClient.get()
                    .uri(uriBuilder -> appendApiKey(uriBuilder.path("/players/{id}/recentMatches")).build(accountId))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return matches != null ? matches : List.of();
        });
    }

    @Cacheable(value = CacheConfig.PLAYER_PEERS_CACHE, key = "#accountId")
    public List<PeerStat> fetchPlayerPeers(long accountId) {
        log.info("Fetching peers for accountId={}", accountId);
        return executeWithRetry("mengambil data teman mabar (peers)", () -> {
            List<PeerStat> peers = restClient.get()
                    .uri(uriBuilder -> appendApiKey(uriBuilder.path("/players/{id}/peers")).build(accountId))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return peers != null ? peers : List.of();
        });
    }

    public ApiHealthStatus checkApiConnection(Long accountId, String endpointType) {
        String endpoint = resolveHealthEndpoint(accountId, endpointType);
        Instant checkedAt = Instant.now();
        long startNanos = System.nanoTime();

        try {
            ResponseEntity<String> response = restClient.get()
                    .uri(uriBuilder -> appendApiKey(uriBuilder.path(endpoint)).build())
                    .retrieve()
                    .toEntity(String.class);

            long latencyMs = (System.nanoTime() - startNanos) / 1_000_000L;
            int statusCode = response.getStatusCode().value();
            return new ApiHealthStatus(
                    "UP",
                    baseUrl,
                    endpoint,
                    statusCode,
                    latencyMs,
                    checkedAt,
                    "Koneksi ke OpenDota API berhasil."
            );
        } catch (RestClientResponseException ex) {
            long latencyMs = (System.nanoTime() - startNanos) / 1_000_000L;
            log.warn("OpenDota health check failed for {} with HTTP {}: {}",
                    endpoint, ex.getStatusCode().value(), ex.getMessage());
            return new ApiHealthStatus(
                    "DOWN",
                    baseUrl,
                    endpoint,
                    ex.getStatusCode().value(),
                    latencyMs,
                    checkedAt,
                    "OpenDota API merespons dengan HTTP " + ex.getStatusCode().value() + ": " + ex.getStatusText()
            );
        } catch (ResourceAccessException ex) {
            long latencyMs = (System.nanoTime() - startNanos) / 1_000_000L;
            log.warn("OpenDota health check timeout/IO error for {}: {}", endpoint, ex.getMessage());
            return new ApiHealthStatus(
                    "DOWN",
                    baseUrl,
                    endpoint,
                    null,
                    latencyMs,
                    checkedAt,
                    "Koneksi atau timeout ke OpenDota API: " + ex.getMessage()
            );
        } catch (RestClientException ex) {
            long latencyMs = (System.nanoTime() - startNanos) / 1_000_000L;
            log.warn("OpenDota health check error for {}: {}", endpoint, ex.getMessage());
            return new ApiHealthStatus(
                    "DOWN",
                    baseUrl,
                    endpoint,
                    null,
                    latencyMs,
                    checkedAt,
                    "Gagal menghubungi OpenDota API: " + ex.getMessage()
            );
        }
    }

    private String resolveHealthEndpoint(Long accountId, String endpointType) {
        if (accountId == null || accountId <= 0) {
            return "/status";
        }
        String normalized = endpointType == null ? "" : endpointType.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "profile" -> "/players/" + accountId;
            case "peers" -> "/players/" + accountId + "/peers";
            default -> "/players/" + accountId + "/recentMatches";
        };
    }

    private <T> T executeWithRetry(String action, Supplier<T> supplier) {
        int attempts = 0;
        while (true) {
            attempts++;
            try {
                return supplier.get();
            } catch (ResourceAccessException | HttpServerErrorException ex) {
                if (attempts <= maxRetries) {
                    log.warn("Transient error saat {} (percobaan {}/{}): {}. Mencoba ulang...",
                            action, attempts, maxRetries + 1, ex.getMessage());
                    sleepBackoff();
                    continue;
                }
                log.error("Failed after {} attempt(s) saat {}", attempts, action, ex);
                throw toFriendlyException(action, ex);
            } catch (RestClientException ex) {
                log.error("Failed saat {}", action, ex);
                throw toFriendlyException(action, ex);
            }
        }
    }

    private void sleepBackoff() {
        try {
            Thread.sleep(RETRY_BACKOFF_MS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private org.springframework.web.util.UriBuilder appendApiKey(
            org.springframework.web.util.UriBuilder uriBuilder) {
        if (!apiKey.isEmpty()) {
            uriBuilder.queryParam("api_key", apiKey);
        }
        return uriBuilder;
    }

    private RuntimeException toFriendlyException(String action, RestClientException ex) {
        if (ex instanceof ResourceAccessException) {
            return new IllegalStateException(
                    "Koneksi ke OpenDota API timeout atau terputus saat " + action
                            + ". Server OpenDota mungkin sedang sibuk, silakan coba beberapa saat lagi.",
                    ex);
        }
        if (ex instanceof RestClientResponseException responseEx) {
            int status = responseEx.getStatusCode().value();
            if (status == HttpStatus.TOO_MANY_REQUESTS.value()) {
                return new IllegalStateException(
                        "Rate limit OpenDota API tercapai. Silakan tunggu beberapa saat lalu coba lagi.", ex);
            }
            if (status == HttpStatus.NOT_FOUND.value()) {
                return new IllegalArgumentException("Data pemain tidak ditemukan di OpenDota.", ex);
            }
            if (status >= 500) {
                return new IllegalStateException(
                        "Server OpenDota API sedang mengalami gangguan (HTTP " + status + "). Silakan coba lagi nanti.",
                        ex);
            }
        }
        return new IllegalStateException("Terjadi kesalahan saat " + action + ": " + ex.getMessage(), ex);
    }
}
