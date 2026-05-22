package com.cwa.projetbdd.models;

import lombok.*;
import java.io.Serializable;
import java.util.Objects;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UtilisateurClassementId implements Serializable {

    private Integer utilisateur;
    private Integer classement;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UtilisateurClassementId u)) return false;
        return Objects.equals(utilisateur, u.utilisateur) && Objects.equals(classement, u.classement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(utilisateur, classement);
    }
}
