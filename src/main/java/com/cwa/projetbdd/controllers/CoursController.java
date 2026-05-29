package com.cwa.projetbdd.controllers;

import com.cwa.projetbdd.dto.DTOs.*;
import com.cwa.projetbdd.exceptions.BusinessException;
import com.cwa.projetbdd.exceptions.ResourceNotFoundException;
import com.cwa.projetbdd.models.Cours;
import com.cwa.projetbdd.repositories.CoursRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController /** annonce deux choses à Spring: « cette classe sert à répondre à des requêtes web » et « tout ce que renvoient ses méthodes doit être transformé en JSON » */
@RequestMapping("/api/cours") /** définit le préfixe d'adresse commun à toutes les méthodes de ce controller*/
@RequiredArgsConstructor  /**C'est ce qui permet à Spring de « brancher » le repository dans le controller sans que tu écrives new CoursRepository(). C'est l'injection de dépendances :
 Spring crée le repository et te le donne tout prêt. Là encore, pas de prévention, juste un raccourci d'écriture.*/

public class CoursController {

    private final CoursRepository coursRepository;

    @GetMapping   /** dit : « cette méthode répond aux requêtes HTTP de type GET » celle ci ne se declanchera pas pour un poste ou un delete */
    public List<CoursDTO> findAll(
            @RequestParam(required = false) String faculte,
            @RequestParam(required = false) Integer credits,
            @RequestParam(required = false) String q) {
        List<Cours> list;
        if (q != null && !q.isBlank())
            list = coursRepository.findByNomContainingIgnoreCaseOrCodeContainingIgnoreCase(q, q);
        else if (faculte != null && !faculte.isBlank())
            list = coursRepository.findByFaculteIgnoreCase(faculte);
        else if (credits != null)
            list = coursRepository.findByCredits(credits);
        else
            list = coursRepository.findAll();
        return list.stream().map(CoursDTO::from).toList();
    }

    @GetMapping("/{code}")
    public CoursDTO findById(@PathVariable String code) {
        return coursRepository.findById(code)
                .map(CoursDTO::from)
                .orElseThrow(() -> new ResourceNotFoundException("Cours " + code + " introuvable"));
    }

    @PostMapping
    public CoursDTO create(@RequestBody CoursDTO dto) {   /**@RequestBody : Spring lit ce JSON et le convertit automatiquement en un objet CoursDTO Java, en faisant
    correspondre chaque clé du JSON ("code", "nom"…) au champ du même nom dans la classe */
        if (dto.getCode() == null || dto.getCode().isBlank())
            throw new BusinessException("Code obligatoire");
        if (coursRepository.existsById(dto.getCode()))
            throw new BusinessException("Code deja existant");
        Cours c = Cours.builder()
                .code(dto.getCode()).nom(dto.getNom()).faculte(dto.getFaculte())
                .credits(dto.getCredits() != null ? dto.getCredits() : 5)
                .anneeAcademique(dto.getAnneeAcademique() != null ? dto.getAnneeAcademique() : "2025-2026")
                .build();
        return CoursDTO.from(coursRepository.save(c));
    }
}
