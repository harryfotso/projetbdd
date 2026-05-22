package com.cwa.projetbdd.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Resume publie par un utilisateur pour un cours specifique.
 * La note moyenne est calculee a la volee (cf. ResumeRepository) et non stockee.
 */
@Entity
@Table(name = "resume")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rid")
    private Integer rid;

    @NotBlank
    @Size(max = 200)
    @Column(name = "titre", nullable = false, length = 200)
    private String titre;

    @NotBlank
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Column(name = "date_publication", nullable = false)
    private LocalDate datePublication;

    @NotNull
    @Min(1)
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 1;

    @NotBlank
    @Pattern(regexp = "public|prive", message = "Visibilite doit etre 'public' ou 'prive'")
    @Column(name = "visibilite", nullable = false, length = 10,
            columnDefinition = "ENUM('public','prive')")
    @Builder.Default
    private String visibilite = "public";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uid", nullable = false)
    private Utilisateur auteur;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "code_cours", nullable = false)
    private Cours cours;

    /**
     * Attribut dérivé — maintenu par trigger.
     * NULL si aucune évaluation, sinon AVG(Evaluation.note) ∈ [0,5].
     */
    @DecimalMin("0.0") @DecimalMax("5.0")
    @Column(name = "note_moyenne", precision = 3, scale = 2)
    private BigDecimal noteMoyenne;
}
