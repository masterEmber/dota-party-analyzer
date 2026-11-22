package com.dimas.dotaanalyzer.service;

import com.dimas.dotaanalyzer.model.DashboardViewModel;
import com.dimas.dotaanalyzer.model.HeroInfo;
import com.dimas.dotaanalyzer.model.PeerStat;
import com.dimas.dotaanalyzer.model.PlayerProfile;
import com.dimas.dotaanalyzer.model.RecentMatch;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PlayerAnalyzerService {

    private static final Logger log = LoggerFactory.getLogger(PlayerAnalyzerService.class);

    public static final int RECENT_MATCHES_LIMIT = 20;
    public static final int TOP_PEERS_LIMIT = 15;
    public static final int MIN_GAMES_FOR_DUO_HIGHLIGHT = 5;

    private final OpenDotaClient openDotaClient;
    private final HeroService heroService;

    public PlayerAnalyzerService(OpenDotaClient openDotaClient, HeroService heroService) {
        this.openDotaClient = openDotaClient;
        this.heroService = heroService;
    }

    public DashboardViewModel analyzePlayer(long accountId) {
        PlayerProfile profile = openDotaClient.fetchPlayerProfile(accountId);
        if (profile == null || !profile.hasProfile()) {
            throw new IllegalArgumentException(
                    "Pemain dengan Steam32 Account ID " + accountId
                            + " tidak ditemukan atau profil Match Data belum dibuka (Expose Public Match Data).");
        }

        List<String> warnings = new ArrayList<>();

        List<RecentMatch> recentMatches = List.of();
        try {
            recentMatches = openDotaClient.fetchRecentMatches(accountId).stream()
                    .limit(RECENT_MATCHES_LIMIT)
                    .toList();
        } catch (IllegalStateException ex) {
            log.warn("Degrading gracefully: failed to fetch recent matches for accountId={}: {}",
                    accountId, ex.getMessage());
            warnings.add("Riwayat 20 match terakhir gagal dimuat dari OpenDota: " + ex.getMessage());
        }

        List<PeerStat> allPeers = List.of();
        try {
            allPeers = openDotaClient.fetchPlayerPeers(accountId);
        } catch (IllegalStateException ex) {
            log.warn("Degrading gracefully: failed to fetch peers for accountId={}: {}",
                    accountId, ex.getMessage());
            warnings.add("Data teman mabar (Party Analyzer) gagal dimuat dari OpenDota: " + ex.getMessage());
        }

        Map<Integer, HeroInfo> heroes = Map.of();
        try {
            heroes = heroService.getAllHeroes();
        } catch (IllegalStateException ex) {
            log.warn("Degrading gracefully: failed to fetch hero constants: {}", ex.getMessage());
            warnings.add("Daftar nama & gambar hero sementara menggunakan fallback ID: " + ex.getMessage());
        }

        int totalRecentGames = recentMatches.size();
        int recentWins = (int) recentMatches.stream().filter(RecentMatch::isWin).count();
        int recentLosses = totalRecentGames - recentWins;
        double recentWinRate = totalRecentGames > 0 ? (recentWins * 100.0) / totalRecentGames : 0.0;

        double avgKills = recentMatches.stream().mapToInt(RecentMatch::kills).average().orElse(0.0);
        double avgDeaths = recentMatches.stream().mapToInt(RecentMatch::deaths).average().orElse(0.0);
        double avgAssists = recentMatches.stream().mapToInt(RecentMatch::assists).average().orElse(0.0);

        List<PeerStat> topPeers = allPeers.stream()
                .filter(peer -> peer.withGames() > 0)
                .sorted(Comparator.comparingInt(PeerStat::withGames).reversed()
                        .thenComparing(Comparator.comparingDouble(PeerStat::winRate).reversed())
                        .thenComparingLong(PeerStat::accountId))
                .limit(TOP_PEERS_LIMIT)
                .toList();

        List<PeerStat> eligibleDuoPeers = allPeers.stream()
                .filter(peer -> peer.withGames() >= MIN_GAMES_FOR_DUO_HIGHLIGHT)
                .toList();

        PeerStat bestDuo = null;
        PeerStat cursedDuo = null;

        if (eligibleDuoPeers.size() == 1) {
            PeerStat onlyPeer = eligibleDuoPeers.get(0);
            if (onlyPeer.winRate() >= 50.0) {
                bestDuo = onlyPeer;
            } else {
                cursedDuo = onlyPeer;
            }
        } else if (eligibleDuoPeers.size() >= 2) {
            bestDuo = eligibleDuoPeers.stream()
                    .max(Comparator.comparingDouble(PeerStat::winRate)
                            .thenComparingInt(PeerStat::withGames))
                    .orElse(null);

            long bestDuoId = bestDuo != null ? bestDuo.accountId() : -1L;
            cursedDuo = eligibleDuoPeers.stream()
                    .filter(peer -> peer.accountId() != bestDuoId)
                    .min(Comparator.comparingDouble(PeerStat::winRate)
                            .thenComparing(Comparator.comparingInt(PeerStat::withGames).reversed()))
                    .orElse(null);
        }

        return new DashboardViewModel(
                accountId,
                profile,
                recentMatches,
                heroes,
                totalRecentGames,
                recentWins,
                recentLosses,
                recentWinRate,
                avgKills,
                avgDeaths,
                avgAssists,
                topPeers,
                bestDuo,
                cursedDuo,
                warnings
        );
    }
}
