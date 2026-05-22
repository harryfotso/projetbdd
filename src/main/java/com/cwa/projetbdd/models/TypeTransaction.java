package com.cwa.projetbdd.models;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Type de transaction de points :
 *  - GAIN_PUBLICATION : gain de points pour avoir publie un resume (+10)
 *  - GAIN_EVALUATION  : gain de points car un autre utilisateur a evalue son resume (+5)
 *  - DEPENSE          : depense de points pour acheter un objet cosmetique (-prix)
 *
 * Les valeurs SQL sont 'Gain Publication', 'Gain Évaluation', 'Dépense'.
 */
public enum TypeTransaction {
    GAIN_PUBLICATION("Gain Publication"),
    GAIN_EVALUATION("Gain Évaluation"),
    DEPENSE("Dépense");

    private final String label;

    TypeTransaction(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    /**
     * Conversion depuis la valeur stockee en base.
     */
    public static TypeTransaction fromDbValue(String value) {
        for (TypeTransaction t : values()) {
            if (t.label.equals(value)) return t;
        }
        throw new IllegalArgumentException("TypeTransaction inconnu : " + value);
    }
}
