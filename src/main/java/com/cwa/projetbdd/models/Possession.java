package com.cwa.projetbdd.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Possession : entite faible representant qu'un utilisateur possede un objet.
 * PK composite (uid, oid) -> garantit qu'on ne possede pas deux fois le meme objet.
 */
@Entity
@Table(name = "possession")
@IdClass(PossessionId.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Possession {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uid", nullable = false)
    private Utilisateur utilisateur;

    @Id
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "oid", nullable = false)
    private Objet objet;

    @NotNull
    @Column(name = "date_achat", nullable = false)
    private LocalDate dateAchat;

    @Column(name = "est_actif", nullable = false)
    @Builder.Default
    private Boolean estActif = false;
}
