package com.dimas.dotaanalyzer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlayerProfile(
        Profile profile,
        @JsonProperty("rank_tier") Integer rankTier,
        @JsonProperty("leaderboard_rank") Integer leaderboardRank
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Profile(
            @JsonProperty("account_id") Long accountId,
            @JsonProperty("personaname") String personaName,
            @JsonProperty("avatarfull") String avatarFull,
            @JsonProperty("profileurl") String profileUrl,
            @JsonProperty("loccountrycode") String countryCode
    ) {}

    public boolean hasProfile() {
        return profile != null && profile.accountId() != null;
    }

    public String displayName() {
        if (profile == null || profile.personaName() == null || profile.personaName().isBlank()) {
            return "Unknown Player";
        }
        return profile.personaName();
    }

    public String rankName() {
        if (rankTier == null || rankTier <= 0) {
            return "Uncalibrated";
        }
        int medal = rankTier / 10;
        int stars = rankTier % 10;
        String tierName = switch (medal) {
            case 1 -> "Herald";
            case 2 -> "Guardian";
            case 3 -> "Crusader";
            case 4 -> "Archon";
            case 5 -> "Legend";
            case 6 -> "Ancient";
            case 7 -> "Divine";
            case 8 -> "Immortal";
            default -> "Uncalibrated";
        };
        if (medal == 8) {
            return leaderboardRank != null ? "Immortal #" + leaderboardRank : "Immortal";
        }
        return stars > 0 ? tierName + " " + stars : tierName;
    }
}
