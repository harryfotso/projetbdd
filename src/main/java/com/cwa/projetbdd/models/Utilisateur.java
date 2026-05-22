package com.cwa.projetbdd.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Utilisateur de la plateforme.
 *
 * Hypotheses :
 *  - 3 cles candidates (UID, Nom, Email), Nom et Email UNIQUE
 *  - Le niveau est calcule dynamiquement via la table seuil_niveau
 *  - Le badge/titre actif est gere via le champ est_actif dans la table possession
 */
@Entity
@Table(name = "utilisateur",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "nom"),
           @UniqueConstraint(columnNames = "email")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "uid")
    private Integer uid;

    @NotBlank
    @Size(min = 3, max = 50)
    @Column(name = "nom", nullable = false, unique = true, length = 50)
    private String nom;

    @NotBlank
    @Email
    @Size(max = 100)
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @NotBlank
    @Column(name = "mot_de_passe_hash", nullable = false, length = 255)
    private String motDePasseHash;

    @NotNull
    @Column(name = "date_inscription", nullable = false)
    private LocalDate dateInscription;

    @NotNull
    @Min(0)
    @Column(name = "points", nullable = false)
    @Builder.Default
    private Integer points = 0;

    /** Attribut dérivé — maintenu par trigger (somme des |montant| pour montant < 0). */
    @NotNull
    @Min(0)
    @Column(name = "total_depense", nullable = false)
    @Builder.Default
    private Integer totalDepense = 0;
}
