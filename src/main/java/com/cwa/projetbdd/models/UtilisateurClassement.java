package com.cwa.projetbdd.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "utilisateur_classement")
@IdClass(UtilisateurClassementId.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UtilisateurClassement {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uid", nullable = false)
    private Utilisateur utilisateur;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cid", nullable = false)
    private Classement classement;

    @NotNull
    @Min(1)
    @Column(name = "place", nullable = false)
    private Integer place;

    @NotNull
    @Min(0)
    @Column(name = "points_au_moment", nullable = false)
    private Integer pointsAuMoment;
}
