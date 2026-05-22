package com.cwa.projetbdd.controllers;

import com.cwa.projetbdd.dto.DTOs.*;
import com.cwa.projetbdd.services.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService service;

    @GetMapping
    public List<EvaluationDTO> findAll(
            @RequestParam(required = false) Integer rid,
            @RequestParam(required = false) Integer evaluateur) {
        if (rid != null) return service.findByResume(rid);
        if (evaluateur != null) return service.findByEvaluateur(evaluateur);
        return List.of();
    }

    @PostMapping
    public EvaluationDTO create(@RequestHeader("X-User-Id") Integer uid,
                                  @RequestBody EvaluationRequest req) {
        return service.create(uid, req);
    }

    @DeleteMapping("/{eid}")
    public ResponseEntity<MessageResponse> delete(@PathVariable Integer eid,
                                                    @RequestHeader("X-User-Id") Integer uid) {
        service.delete(eid, uid);
        return ResponseEntity.ok(new MessageResponse("Evaluation supprimee"));
    }
}
