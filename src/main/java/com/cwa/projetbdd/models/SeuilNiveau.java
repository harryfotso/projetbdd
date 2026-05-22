package com.cwa.projetbdd.models;

import jakarta.persistence.*;
import lombok.*;

/**
 * Table de seuils pour le calcul dynamique du niveau d'un utilisateur.
 * Le niveau est le MAX(niveau) tel que points_min <= utilisateur.points.
 */
@Entity
@Table(name = "seuil_niveau")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SeuilNiveau {

    @Id
    @Column(name = "points_min")
    private Integer pointsMin;

    @Column(name = "niveau", nullable = false, unique = true)
    private Integer niveau;
}
