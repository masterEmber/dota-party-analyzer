package com.dimas.dotaanalyzer.service;

import com.dimas.dotaanalyzer.model.DashboardViewModel;
import com.dimas.dotaanalyzer.model.HeroInfo;
import com.dimas.dotaanalyzer.model.PeerStat;
import com.dimas.dotaanalyzer.model.PlayerProfile;
import com.dimas.dotaanalyzer.model.RecentMatch;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PlayerAnalyzerService {

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

        List<RecentMatch> recentMatches = openDotaClient.fetchRecentMatches(accountId).stream()
                .limit(RECENT_MATCHES_LIMIT)
                .toList();

        List<PeerStat> allPeers = openDotaClient.fetchPlayerPeers(accountId);
        Map<Integer, HeroInfo> heroes = heroService.getAllHeroes();

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
                cursedDuo
        );
    }
}
