package com.cwa.projetbdd.controllers;

import com.cwa.projetbdd.dto.DTOs.*;
import com.cwa.projetbdd.exceptions.ResourceNotFoundException;
import com.cwa.projetbdd.models.TypeObjet;
import com.cwa.projetbdd.repositories.ObjetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/objets")
@RequiredArgsConstructor
public class ObjetController {

    private final ObjetRepository repo;

    @GetMapping
    public List<ObjetDTO> findAll(@RequestParam(required = false) String type) {
        var list = (type != null && !type.isBlank())
                ? repo.findByType(TypeObjet.valueOf(type))
                : repo.findAll();
        return list.stream().map(ObjetDTO::from).toList();
    }

    @GetMapping("/{oid}")
    public ObjetDTO findById(@PathVariable Integer oid) {
        return repo.findById(oid).map(ObjetDTO::from)
                .orElseThrow(() -> new ResourceNotFoundException("Objet introuvable"));
    }
}
