package com.dimas.dotaanalyzer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HeroInfo(
        int id,
        String name,
        @JsonProperty("localized_name") String localizedName,
        @JsonProperty("primary_attr") String primaryAttr,
        @JsonProperty("attack_type") String attackType,
        List<String> roles,
        String img,
        String icon
) {
    private static final String STEAM_CDN_BASE = "https://cdn.cloudflare.steamstatic.com";

    public static HeroInfo unknown(int heroId) {
        return new HeroInfo(heroId, "unknown_" + heroId, "Hero #" + heroId, null, null, List.of(), null, null);
    }

    public String imageUrl() {
        if (img == null || img.isBlank()) {
            return "";
        }
        return img.startsWith("http") ? img : STEAM_CDN_BASE + img;
    }

    public String iconUrl() {
        if (icon == null || icon.isBlank()) {
            return imageUrl();
        }
        return icon.startsWith("http") ? icon : STEAM_CDN_BASE + icon;
    }
}
