package com.dimas.dotaanalyzer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Locale;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PeerStat(
        @JsonProperty("account_id") long accountId,
        @JsonProperty("personaname") String personaName,
        @JsonProperty("avatarfull") String avatarFull,
        @JsonProperty("with_games") int withGames,
        @JsonProperty("with_win") int withWin,
        @JsonProperty("last_played") long lastPlayed
) {
    public String displayName() {
        return (personaName == null || personaName.isBlank()) ? "Player #" + accountId : personaName;
    }

    public int losses() {
        return Math.max(0, withGames - withWin);
    }

    public double winRate() {
        if (withGames <= 0) {
            return 0.0;
        }
        return (withWin * 100.0) / withGames;
    }

    public String formattedWinRate() {
        return String.format(Locale.US, "%.1f%%", winRate());
    }
}
