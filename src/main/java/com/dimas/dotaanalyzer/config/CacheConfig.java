package com.dimas.dotaanalyzer.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String HEROES_CACHE = "heroes";
    public static final String PLAYER_PROFILE_CACHE = "playerProfile";
    public static final String PLAYER_PEERS_CACHE = "playerPeers";
    public static final String RECENT_MATCHES_CACHE = "recentMatches";

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
                buildCache(HEROES_CACHE, Duration.ofHours(24), 10),
                buildCache(PLAYER_PROFILE_CACHE, Duration.ofMinutes(10), 500),
                buildCache(PLAYER_PEERS_CACHE, Duration.ofMinutes(10), 500),
                buildCache(RECENT_MATCHES_CACHE, Duration.ofMinutes(5), 500)
        ));
        return cacheManager;
    }

    private CaffeineCache buildCache(String name, Duration ttl, long maxSize) {
        return new CaffeineCache(
                name,
                Caffeine.newBuilder()
                        .expireAfterWrite(ttl)
                        .maximumSize(maxSize)
                        .recordStats()
                        .build()
        );
    }
}
