package com.cwa.projetbdd.models;

import lombok.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * Cle composite pour l'entite Possession (UID + OID).
 * Doit implementer Serializable et redefinir equals/hashCode (JPA).
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PossessionId implements Serializable {

    private Integer utilisateur;
    private Integer objet;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PossessionId p)) return false;
        return Objects.equals(utilisateur, p.utilisateur) && Objects.equals(objet, p.objet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(utilisateur, objet);
    }
}
