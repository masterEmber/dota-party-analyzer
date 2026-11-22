package com.dimas.dotaanalyzer.service;

import com.dimas.dotaanalyzer.config.CacheConfig;
import com.dimas.dotaanalyzer.model.HeroInfo;
import com.dimas.dotaanalyzer.model.PeerStat;
import com.dimas.dotaanalyzer.model.PlayerProfile;
import com.dimas.dotaanalyzer.model.RecentMatch;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class OpenDotaClient {

    private static final Logger log = LoggerFactory.getLogger(OpenDotaClient.class);

    private final RestClient restClient;

    public OpenDotaClient(RestClient openDotaRestClient) {
        this.restClient = openDotaRestClient;
    }

    @Cacheable(value = CacheConfig.HEROES_CACHE, key = "'all'")
    public Map<Integer, HeroInfo> fetchHeroes() {
        log.info("Fetching heroes constants from OpenDota API");
        try {
            Map<String, HeroInfo> response = restClient.get()
                    .uri("/constants/heroes")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || response.isEmpty()) {
                return Collections.emptyMap();
            }
            return response.values().stream()
                    .collect(Collectors.toMap(HeroInfo::id, Function.identity(), (a, b) -> a));
        } catch (RestClientException ex) {
            log.error("Failed to fetch heroes from OpenDota API", ex);
            throw toFriendlyException("gagal mengambil daftar hero dari OpenDota", ex);
        }
    }

    @Cacheable(value = CacheConfig.PLAYER_PROFILE_CACHE, key = "#accountId")
    public PlayerProfile fetchPlayerProfile(long accountId) {
        log.info("Fetching player profile for accountId={}", accountId);
        try {
            return restClient.get()
                    .uri("/players/{id}", accountId)
                    .retrieve()
                    .body(PlayerProfile.class);
        } catch (RestClientException ex) {
            log.error("Failed to fetch player profile for accountId={}", accountId, ex);
            throw toFriendlyException("gagal mengambil profil pemain", ex);
        }
    }

    @Cacheable(value = CacheConfig.RECENT_MATCHES_CACHE, key = "#accountId")
    public List<RecentMatch> fetchRecentMatches(long accountId) {
        log.info("Fetching recent matches for accountId={}", accountId);
        try {
            List<RecentMatch> matches = restClient.get()
                    .uri("/players/{id}/recentMatches", accountId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return matches != null ? matches : List.of();
        } catch (RestClientException ex) {
            log.error("Failed to fetch recent matches for accountId={}", accountId, ex);
            throw toFriendlyException("gagal mengambil riwayat match terakhir", ex);
        }
    }

    @Cacheable(value = CacheConfig.PLAYER_PEERS_CACHE, key = "#accountId")
    public List<PeerStat> fetchPlayerPeers(long accountId) {
        log.info("Fetching peers for accountId={}", accountId);
        try {
            List<PeerStat> peers = restClient.get()
                    .uri("/players/{id}/peers", accountId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return peers != null ? peers : List.of();
        } catch (RestClientException ex) {
            log.error("Failed to fetch peers for accountId={}", accountId, ex);
            throw toFriendlyException("gagal mengambil data teman mabar (peers)", ex);
        }
    }

    private RuntimeException toFriendlyException(String action, RestClientException ex) {
        if (ex instanceof RestClientResponseException responseEx) {
            if (responseEx.getStatusCode().value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
                return new IllegalStateException(
                        "Rate limit OpenDota API tercapai. Silakan tunggu beberapa saat lalu coba lagi.", ex);
            }
            if (responseEx.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                return new IllegalArgumentException("Data pemain tidak ditemukan di OpenDota.", ex);
            }
        }
        return new IllegalStateException("Terjadi kesalahan saat " + action + ": " + ex.getMessage(), ex);
    }
}
