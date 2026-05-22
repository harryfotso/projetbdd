package com.cwa.projetbdd.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Evaluation (note + commentaire) qu'un utilisateur depose sur un resume.
 * Contraintes :
 *   - UNIQUE(uid, rid) : un user ne peut evaluer qu'une fois chaque resume
 *   - uid != Resume.uid : pas d'auto-evaluation (verifie en service + trigger SQL)
 */
@Entity
@Table(name = "evaluation",
       uniqueConstraints = @UniqueConstraint(columnNames = {"uid", "rid"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "eid")
    private Integer eid;

    @NotNull
    @DecimalMin("0.0") @DecimalMax("5.0")
    @Column(name = "note", nullable = false, precision = 2, scale = 1)
    private BigDecimal note;

    @Column(name = "commentaire", columnDefinition = "TEXT")
    private String commentaire;

    @NotNull
    @Column(name = "date_eval", nullable = false)
    private LocalDate dateEval;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uid", nullable = false)
    private Utilisateur evaluateur;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rid", nullable = false)
    private Resume resume;
}
