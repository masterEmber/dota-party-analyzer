package com.dimas.dotaanalyzer.controller;

import com.dimas.dotaanalyzer.model.DashboardViewModel;
import com.dimas.dotaanalyzer.service.PlayerAnalyzerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final PlayerAnalyzerService playerAnalyzerService;

    public DashboardController(PlayerAnalyzerService playerAnalyzerService) {
        this.playerAnalyzerService = playerAnalyzerService;
    }

    @GetMapping("/")
    public String home(
            @RequestParam(name = "accountId", required = false) String accountId,
            Model model) {
        if (accountId != null && !accountId.isBlank()) {
            String trimmed = accountId.trim();
            if (!trimmed.matches("\\d+")) {
                model.addAttribute("searchAccountId", trimmed);
                model.addAttribute("errorMessage",
                        "Steam32 Account ID harus berupa angka positif (contoh: 86745912).");
                return "dashboard";
            }
            long parsedId = Long.parseLong(trimmed);
            if (parsedId <= 0) {
                model.addAttribute("searchAccountId", trimmed);
                model.addAttribute("errorMessage", "Steam32 Account ID harus lebih besar dari 0.");
                return "dashboard";
            }
            return "redirect:/player/" + parsedId;
        }
        return "dashboard";
    }

    @GetMapping("/player/{accountId}")
    public String playerDashboard(@PathVariable long accountId, Model model) {
        model.addAttribute("searchAccountId", String.valueOf(accountId));
        if (accountId <= 0) {
            model.addAttribute("errorMessage", "Steam32 Account ID tidak valid.");
            return "dashboard";
        }
        try {
            DashboardViewModel dashboard = playerAnalyzerService.analyzePlayer(accountId);
            model.addAttribute("dashboard", dashboard);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Unable to render dashboard for accountId={}: {}", accountId, ex.getMessage());
            model.addAttribute("errorMessage", ex.getMessage());
        }
        return "dashboard";
    }
}
