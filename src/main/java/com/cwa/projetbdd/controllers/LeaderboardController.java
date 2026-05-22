package com.cwa.projetbdd.controllers;

import com.cwa.projetbdd.dto.DTOs.*;
import com.cwa.projetbdd.services.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final AnalyticsService analyticsService;

    @GetMapping
    public List<LeaderboardEntry> get(@RequestParam(required = false) Integer top) {
        if (top != null && top > 0) {
            var all = analyticsService.leaderboardComplet();
            return all.subList(0, Math.min(top, all.size()));
        }
        return analyticsService.leaderboardComplet();
    }
}
