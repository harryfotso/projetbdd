package com.cwa.projetbdd.models;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Convertit entre l'enum Java TypeTransaction et la valeur ENUM MySQL.
 * Java : GAIN_PUBLICATION / GAIN_EVALUATION / DEPENSE
 * MySQL : 'Gain Publication' / 'Gain Évaluation' / 'Dépense'
 */
@Converter(autoApply = true)
public class TypeTransactionConverter implements AttributeConverter<TypeTransaction, String> {

    @Override
    public String convertToDatabaseColumn(TypeTransaction attribute) {
        return attribute == null ? null : attribute.getLabel();
    }

    @Override
    public TypeTransaction convertToEntityAttribute(String dbData) {
        return dbData == null ? null : TypeTransaction.fromDbValue(dbData);
    }
}
