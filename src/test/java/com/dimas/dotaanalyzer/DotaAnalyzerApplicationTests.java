package com.dimas.dotaanalyzer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.dimas.dotaanalyzer.config.CacheConfig;
import com.dimas.dotaanalyzer.model.DashboardViewModel;
import com.dimas.dotaanalyzer.model.HeroInfo;
import com.dimas.dotaanalyzer.model.PeerStat;
import com.dimas.dotaanalyzer.model.PlayerProfile;
import com.dimas.dotaanalyzer.model.RecentMatch;
import com.dimas.dotaanalyzer.service.OpenDotaClient;
import com.dimas.dotaanalyzer.service.PlayerAnalyzerService;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DotaAnalyzerApplicationTests {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private PlayerAnalyzerService playerAnalyzerService;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OpenDotaClient openDotaClient;

    @Test
    void contextLoadsAndCachesAreConfigured() {
        assertThat(cacheManager.getCache(CacheConfig.HEROES_CACHE)).isNotNull();
        assertThat(cacheManager.getCache(CacheConfig.PLAYER_PROFILE_CACHE)).isNotNull();
        assertThat(cacheManager.getCache(CacheConfig.PLAYER_PEERS_CACHE)).isNotNull();
        assertThat(cacheManager.getCache(CacheConfig.RECENT_MATCHES_CACHE)).isNotNull();
    }

    @Test
    void analyzePlayerComputesRecentStatsTopPeersAndDuoHighlights() {
        long accountId = 86745912L;

        PlayerProfile profile = new PlayerProfile(
                new PlayerProfile.Profile(accountId, "DimasCarry", "https://example.com/avatar.png",
                        "https://steamcommunity.com/id/dimas", "ID"),
                64,
                null
        );

        // 22 matches returned -> service should cap at 20 matches (12 wins, 8 losses)
        List<RecentMatch> matches = IntStream.range(0, 22)
                .mapToObj(i -> {
                    boolean radiantPlayer = (i % 2 == 0);
                    int playerSlot = radiantPlayer ? 0 : 128;
                    boolean shouldWin = i < 12;
                    boolean radiantWin = radiantPlayer == shouldWin;
                    return new RecentMatch(
                            1000L + i, playerSlot, radiantWin, 2400, 22, 7, 1, 1730000000L + i,
                            10, 4, 8, 600, 700, 2
                    );
                })
                .toList();

        // Peers: 20 total peers, including < 5 games outliers that must be excluded from Best/Cursed Duo
        List<PeerStat> peers = new java.util.ArrayList<>();
        peers.add(new PeerStat(1L, "OneGameWonder", null, 1, 1, 1730000000L)); // 100% WR but < 5 games
        peers.add(new PeerStat(2L, "OneGameLoss", null, 2, 0, 1730000000L));   // 0% WR but < 5 games
        peers.add(new PeerStat(3L, "BestSupport", null, 10, 8, 1730000000L));  // 80% WR (>= 5 games) -> Best Duo
        peers.add(new PeerStat(4L, "CursedMid", null, 8, 2, 1730000000L));     // 25% WR (>= 5 games) -> Cursed Duo
        for (int i = 5; i <= 20; i++) {
            peers.add(new PeerStat(i, "Friend" + i, null, 6 + i, 3 + (i / 2), 1730000000L));
        }

        Map<Integer, HeroInfo> heroes = Map.of(
                1, new HeroInfo(1, "npc_dota_hero_antimage", "Anti-Mage", "agi", "Melee",
                        List.of("Carry"), "/apps/dota2/images/dota_react/heroes/antimage.png", null)
        );

        when(openDotaClient.fetchPlayerProfile(accountId)).thenReturn(profile);
        when(openDotaClient.fetchRecentMatches(accountId)).thenReturn(matches);
        when(openDotaClient.fetchPlayerPeers(accountId)).thenReturn(peers);
        when(openDotaClient.fetchHeroes()).thenReturn(heroes);

        DashboardViewModel vm = playerAnalyzerService.analyzePlayer(accountId);

        assertThat(vm.profile().rankName()).isEqualTo("Ancient 4");
        assertThat(vm.totalRecentGames()).isEqualTo(20);
        assertThat(vm.recentWins()).isEqualTo(12);
        assertThat(vm.recentLosses()).isEqualTo(8);
        assertThat(vm.recentWinRate()).isEqualTo(60.0);
        assertThat(vm.topPeers()).hasSize(15);
        assertThat(vm.bestDuo()).isNotNull();
        assertThat(vm.bestDuo().personaName()).isEqualTo("BestSupport");
        assertThat(vm.cursedDuo()).isNotNull();
        assertThat(vm.cursedDuo().personaName()).isEqualTo("CursedMid");
        assertThat(vm.heroName(1)).isEqualTo("Anti-Mage");
    }

    @Test
    void analyzePlayerThrowsWhenProfileMissing() {
        long missingId = 999999999L;
        when(openDotaClient.fetchPlayerProfile(missingId)).thenReturn(new PlayerProfile(null, null, null));

        assertThatThrownBy(() -> playerAnalyzerService.analyzePlayer(missingId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tidak ditemukan");
    }

    @Test
    void dashboardControllerRoutesWork() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));

        mockMvc.perform(get("/").param("accountId", "86745912"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/player/86745912"));

        long accountId = 86745912L;
        PlayerProfile profile = new PlayerProfile(
                new PlayerProfile.Profile(accountId, "Arteezy", null, null, "CA"),
                80,
                5
        );
        when(openDotaClient.fetchPlayerProfile(accountId)).thenReturn(profile);
        when(openDotaClient.fetchRecentMatches(accountId)).thenReturn(List.of());
        when(openDotaClient.fetchPlayerPeers(accountId)).thenReturn(List.of());
        when(openDotaClient.fetchHeroes()).thenReturn(Map.of());

        mockMvc.perform(get("/player/{id}", accountId))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Arteezy")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Immortal #5")));
    }
}
