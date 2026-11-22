package com.dimas.dotaanalyzer.service;

import com.dimas.dotaanalyzer.model.HeroInfo;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class HeroService {

    private final OpenDotaClient openDotaClient;

    public HeroService(OpenDotaClient openDotaClient) {
        this.openDotaClient = openDotaClient;
    }

    public Map<Integer, HeroInfo> getAllHeroes() {
        return openDotaClient.fetchHeroes();
    }

    public HeroInfo getHeroById(int heroId) {
        return getAllHeroes().getOrDefault(heroId, HeroInfo.unknown(heroId));
    }
}
