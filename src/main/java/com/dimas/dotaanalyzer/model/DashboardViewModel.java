package com.dimas.dotaanalyzer.model;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public record DashboardViewModel(
        long accountId,
        PlayerProfile profile,
        List<RecentMatch> recentMatches,
        Map<Integer, HeroInfo> heroes,
        int totalRecentGames,
        int recentWins,
        int recentLosses,
        double recentWinRate,
        double avgKills,
        double avgDeaths,
        double avgAssists,
        List<PeerStat> topPeers,
        PeerStat bestDuo,
        PeerStat cursedDuo,
        List<String> warnings
) {
    public DashboardViewModel {
        recentMatches = recentMatches != null ? recentMatches : List.of();
        heroes = heroes != null ? heroes : Map.of();
        topPeers = topPeers != null ? topPeers : List.of();
        warnings = warnings != null ? List.copyOf(warnings) : List.of();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public String heroName(int heroId) {
        HeroInfo info = heroes.get(heroId);
        return info != null ? info.localizedName() : "Hero #" + heroId;
    }

    public String heroImageUrl(int heroId) {
        HeroInfo info = heroes.get(heroId);
        return info != null ? info.imageUrl() : "";
    }

    public String formattedRecentWinRate() {
        return String.format(Locale.US, "%.1f%%", recentWinRate);
    }

    public String formattedAvgKda() {
        return String.format(Locale.US, "%.1f / %.1f / %.1f", avgKills, avgDeaths, avgAssists);
    }
}
