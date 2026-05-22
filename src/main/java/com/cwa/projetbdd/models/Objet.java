package com.cwa.projetbdd.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Objet cosmetique disponible a l'achat dans la boutique.
 * Peut etre un badge, titre, theme ou cosmetique.
 */
@Entity
@Table(name = "objet")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Objet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oid")
    private Integer oid;

    @NotBlank
    @Size(max = 100)
    @Column(name = "nom", nullable = false, unique = true, length = 100)
    private String nom;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Min(0)
    @Column(name = "prix", nullable = false)
    private Integer prix;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TypeObjet type;

    /** Attribut dérivé — maintenu par trigger (COUNT tuples dans Possession). */
    @NotNull
    @Min(0)
    @Column(name = "nb_achats", nullable = false)
    @Builder.Default
    private Integer nbAchats = 0;
}
