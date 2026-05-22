package com.cwa.projetbdd.controllers;

import com.cwa.projetbdd.dto.DTOs.*;
import com.cwa.projetbdd.services.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller pour les 8 requetes demandees par l'enonce.
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService service;

    /** Q1 : Top 10 utilisateurs par points */
    @GetMapping("/top10")
    public List<LeaderboardEntry> top10() { return service.top10ParPoints(); }

    /** Q2 : Utilisateurs ayant publie dans 3+ cours differents */
    @GetMapping("/multi-cours")
    public List<UtilisateurDTO> multiCours() { return service.utilisateursMultiCours(); }

    /** Q3 : Cours ayant le plus de resumes publies */
    @GetMapping("/cours-populaire")
    public CoursPlusResumes coursPopulaire() { return service.coursLePlusResume(); }

    /** Q4 : Resumes les mieux notes pour chaque cours */
    @GetMapping("/best-resumes")
    public List<MeilleurResumeParCours> bestResumes() { return service.meilleursResumesParCours(); }

    /** Q5 : Utilisateurs n'ayant jamais publie de resume */
    @GetMapping("/no-resume")
    public List<UtilisateurDTO> sansResume() { return service.utilisateursSansResume(); }

    /** Q6 : Objet cosmetique le plus achete */
    @GetMapping("/objet-populaire")
    public ObjetPlusAchete objetPopulaire() { return service.objetLePlusAchete(); }

    /** Q7 : Utilisateurs ayant depense plus que disponible */
    @GetMapping("/overspenders")
    public List<Overspender> overspenders() { return service.overspenders(); }

    /** Q8 : Nombre moyen de resumes publies par utilisateur */
    @GetMapping("/avg-resumes")
    public Map<String, Double> avgResumes() {
        return Map.of("moyenne", service.moyenneResumesParUtilisateur());
    }
}
