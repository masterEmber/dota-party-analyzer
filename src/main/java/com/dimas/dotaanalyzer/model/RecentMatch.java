package com.dimas.dotaanalyzer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RecentMatch(
        @JsonProperty("match_id") long matchId,
        @JsonProperty("player_slot") int playerSlot,
        @JsonProperty("radiant_win") boolean radiantWin,
        int duration,
        @JsonProperty("game_mode") int gameMode,
        @JsonProperty("lobby_type") int lobbyType,
        @JsonProperty("hero_id") int heroId,
        @JsonProperty("start_time") long startTime,
        int kills,
        int deaths,
        int assists,
        @JsonProperty("gold_per_min") int goldPerMin,
        @JsonProperty("xp_per_min") int xpPerMin,
        @JsonProperty("party_size") Integer partySize
) {
    private static final DateTimeFormatter MATCH_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH).withZone(ZoneId.systemDefault());

    public boolean isRadiant() {
        return playerSlot < 128;
    }

    public boolean isWin() {
        return isRadiant() == radiantWin;
    }

    public String formattedDuration() {
        int minutes = duration / 60;
        int seconds = duration % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    public double kdaRatio() {
        int effectiveDeaths = Math.max(deaths, 1);
        return (kills + assists) / (double) effectiveDeaths;
    }

    public String formattedKdaRatio() {
        return String.format(Locale.US, "%.2f", kdaRatio());
    }

    public String formattedStartTime() {
        if (startTime <= 0) {
            return "-";
        }
        return MATCH_DATE_FORMATTER.format(Instant.ofEpochSecond(startTime));
    }

    public String partyLabel() {
        if (partySize == null || partySize <= 0) {
            return "-";
        }
        return partySize == 1 ? "Solo" : "Party (" + partySize + ")";
    }
}
