package com.cwa.projetbdd.controllers;

import com.cwa.projetbdd.dto.DTOs.*;
import com.cwa.projetbdd.services.PossessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/possessions")
@RequiredArgsConstructor
public class PossessionController {

    private final PossessionService service;

    @GetMapping
    public List<PossessionDTO> findByUser(@RequestParam Integer uid) {
        return service.findByUtilisateur(uid);
    }

    @PostMapping
    public PossessionDTO acheter(@RequestBody AchatRequest req) {
        return service.acheter(req);
    }
}
