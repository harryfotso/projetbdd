package com.cwa.projetbdd.controllers;

import com.cwa.projetbdd.dto.DTOs.*;
import com.cwa.projetbdd.services.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService service;

    @GetMapping
    public List<ResumeDTO> findAll(
            @RequestParam(required = false) String cours,
            @RequestParam(required = false) Integer auteur) {
        if (cours != null) return service.findByCours(cours);
        if (auteur != null) return service.findByAuteur(auteur);
        return service.findAll();
    }

    @GetMapping("/{rid}")
    public ResumeDTO findById(@PathVariable Integer rid) { return service.findById(rid); }

    /** Publication. Header X-User-Id requis (utilisateur connecte). */
    @PostMapping
    public ResumeDTO publish(@RequestHeader("X-User-Id") Integer uid,
                              @RequestBody ResumeRequest req) {
        return service.publish(uid, req);
    }

    @PutMapping("/{rid}")
    public ResumeDTO update(@PathVariable Integer rid,
                             @RequestHeader("X-User-Id") Integer uid,
                             @RequestBody ResumeRequest req) {
        return service.update(rid, uid, req);
    }

    @DeleteMapping("/{rid}")
    public ResponseEntity<MessageResponse> delete(@PathVariable Integer rid,
                                                    @RequestHeader("X-User-Id") Integer uid) {
        service.delete(rid, uid);
        return ResponseEntity.ok(new MessageResponse("Resume supprime"));
    }
}
