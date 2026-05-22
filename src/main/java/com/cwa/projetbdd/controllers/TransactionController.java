package com.cwa.projetbdd.controllers;

import com.cwa.projetbdd.dto.DTOs.*;
import com.cwa.projetbdd.repositories.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionRepository repo;

    @GetMapping
    public List<TransactionDTO> findByUser(@RequestParam Integer uid) {
        return repo.findByUtilisateurUidOrderByDateTransactionDesc(uid).stream()
                .map(TransactionDTO::from).toList();
    }
}
