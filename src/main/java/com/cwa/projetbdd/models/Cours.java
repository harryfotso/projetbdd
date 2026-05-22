package com.cwa.projetbdd.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Cours universitaire identifie par son code unique.
 * AnneeAcademique est conservee comme attribut (pas dans la PK)
 * pour simplifier les FK et les jointures.
 */
@Entity
@Table(name = "cours")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Cours {

    @Id
    @Size(max = 20)
    @Column(name = "code", length = 20)
    private String code;

    @NotBlank
    @Size(max = 100)
    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @NotBlank
    @Size(max = 80)
    @Column(name = "faculte", nullable = false, length = 80)
    private String faculte;

    @NotNull
    @Min(1)
    @Column(name = "credits", nullable = false)
    private Integer credits;

    @NotBlank
    @Size(max = 9)
    @Column(name = "annee_academique", nullable = false, length = 9)
    @Builder.Default
    private String anneeAcademique = "2025-2026";

    /** Attribut dérivé — maintenu par trigger (COUNT résumés du cours). */
    @NotNull
    @Min(0)
    @Column(name = "nb_resumes", nullable = false)
    @Builder.Default
    private Integer nbResumes = 0;
}
