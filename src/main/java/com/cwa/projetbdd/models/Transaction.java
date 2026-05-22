package com.cwa.projetbdd.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Transaction de points.
 * Exclusion mutuelle : exactement un parmi {resume, evaluation, oid} est non-null.
 *  - Gain Publication  : montant > 0, lien vers Resume
 *  - Gain Évaluation   : montant > 0, lien vers Evaluation (le beneficiaire est l'auteur du resume)
 *  - Dépense           : montant < 0, lien vers Possession (via utilisateur_uid + oid)
 *
 * NB : nom de table 'transaction_' (TRANSACTION est mot reserve MySQL).
 * La FK composite (utilisateur_uid, oid) -> possession(uid, oid) est geree au niveau DDL.
 * Cote JPA, on garde oid comme simple Integer et objet comme @ManyToOne vers Objet
 * pour la commodite de lecture.
 */
@Entity
@Table(name = "transaction_")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tid")
    private Integer tid;

    @NotNull
    @Column(name = "type_transaction", nullable = false)
    @Convert(converter = TypeTransactionConverter.class)
    private TypeTransaction typeTransaction;

    @NotNull
    @Column(name = "date_transaction", nullable = false)
    private LocalDate dateTransaction;

    @NotNull
    @Column(name = "montant", nullable = false)
    private Integer montant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_uid", nullable = false)
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rid")
    private Resume resume;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eid")
    private Evaluation evaluation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oid", insertable = true, updatable = true)
    private Objet objet;
}
