package com.cwa.projetbdd.controllers;

import com.cwa.projetbdd.dto.DTOs.*;
import com.cwa.projetbdd.services.UtilisateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/utilisateurs")
@RequiredArgsConstructor
public class UtilisateurController {

    private final UtilisateurService service;

    @GetMapping
    public List<UtilisateurDTO> findAll() { return service.findAll(); }

    @GetMapping("/{uid}")
    public UtilisateurDTO findById(@PathVariable Integer uid) { return service.findById(uid); }

    @GetMapping("/by-nom/{nom}")
    public UtilisateurDTO findByNom(@PathVariable String nom) { return service.findByNom(nom); }

    @PostMapping("/register")
    public UtilisateurDTO register(@RequestBody RegisterRequest req) { return service.register(req); }

    @PostMapping("/login")
    public UtilisateurDTO login(@RequestBody LoginRequest req) { return service.login(req); }

    @PutMapping("/{uid}")
    public UtilisateurDTO update(@PathVariable Integer uid, @RequestBody UtilisateurUpdateRequest req) {
        return service.update(uid, req);
    }

    @DeleteMapping("/{uid}")
    public ResponseEntity<MessageResponse> delete(@PathVariable Integer uid) {
        service.delete(uid);
        return ResponseEntity.ok(new MessageResponse("Utilisateur supprime"));
    }
}
