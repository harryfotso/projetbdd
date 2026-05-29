/**
 * INFOH303 - Projet BDD.
 * <p>
 * Script DDL de création du schéma relationnel.
 * Aligné avec les modèles Java (JPA/Hibernate) — MySQL 8 / Spring Boot 3.3.
 * </p>
 */

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS transaction_;
DROP TABLE IF EXISTS utilisateur_classement;
DROP TABLE IF EXISTS classement;
DROP TABLE IF EXISTS possession;
DROP TABLE IF EXISTS evaluation;
DROP TABLE IF EXISTS resume;
DROP TABLE IF EXISTS objet;
DROP TABLE IF EXISTS seuil_niveau;
DROP TABLE IF EXISTS utilisateur;
DROP TABLE IF EXISTS cours;

SET FOREIGN_KEY_CHECKS = 1;

/**
 * Table des cours universitaires.
 * <p>
 * Identifiée par un code unique. L'attribut dérivé {@code nb_resumes}
 * est maintenu automatiquement par trigger.
 * </p>
 */
CREATE TABLE cours (
                       code            VARCHAR(20)  NOT NULL,
                       nom             VARCHAR(100) NOT NULL,
                       faculte         VARCHAR(80)  NOT NULL,
                       credits         INT          NOT NULL,
                       annee_academique VARCHAR(9)  NOT NULL DEFAULT '2025-2026',
                       nb_resumes      INT          NOT NULL DEFAULT 0,

                       CONSTRAINT pk_cours PRIMARY KEY (code),
                       CONSTRAINT chk_cours_credits  CHECK (credits >= 1),
                       CONSTRAINT chk_cours_nb_res   CHECK (nb_resumes >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/**
 * Table des utilisateurs de la plateforme.
 * <p>
 * Trois clés candidates : {@code uid}, {@code nom} (UNIQUE), {@code email} (UNIQUE).
 * L'attribut dérivé {@code total_depense} est maintenu automatiquement par trigger.
 * </p>
 */
CREATE TABLE utilisateur (
                             uid                 INT          NOT NULL AUTO_INCREMENT,
                             nom                 VARCHAR(50)  NOT NULL,
                             email               VARCHAR(100) NOT NULL,
                             mot_de_passe_hash   VARCHAR(255) NOT NULL,
                             date_inscription    DATE         NOT NULL,
                             points              INT          NOT NULL DEFAULT 0,
                             total_depense       INT          NOT NULL DEFAULT 0,

                             CONSTRAINT pk_utilisateur  PRIMARY KEY (uid),
                             CONSTRAINT uq_utilisateur_nom   UNIQUE (nom),
                             CONSTRAINT uq_utilisateur_email UNIQUE (email),
                             CONSTRAINT chk_utilisateur_points  CHECK (points >= 0),
                             CONSTRAINT chk_utilisateur_depense CHECK (total_depense >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/**
 * Table des seuils de niveau.
 * <p>
 * Définit les paliers de points nécessaires pour chaque niveau.
 * La clé primaire est {@code points_min}.
 * </p>
 */
CREATE TABLE seuil_niveau (
                              points_min  INT NOT NULL,
                              niveau      INT NOT NULL,

                              CONSTRAINT pk_seuil_niveau   PRIMARY KEY (points_min),
                              CONSTRAINT uq_seuil_niveau   UNIQUE (niveau),
                              CONSTRAINT chk_seuil_points  CHECK (points_min >= 0),
                              CONSTRAINT chk_seuil_niveau  CHECK (niveau >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/**
 * Table des objets cosmétiques disponibles à l'achat.
 * <p>
 * Types possibles : badge, titre, theme, cosmetique.
 * L'attribut dérivé {@code nb_achats} est maintenu automatiquement par trigger.
 * </p>
 */
CREATE TABLE objet (
                       oid         INT          NOT NULL AUTO_INCREMENT,
                       nom         VARCHAR(100) NOT NULL,
                       description TEXT,
                       prix        INT          NOT NULL,
                       type        ENUM('badge','titre','theme','cosmetique') NOT NULL,
                       nb_achats   INT          NOT NULL DEFAULT 0,

                       CONSTRAINT pk_objet      PRIMARY KEY (oid),
                       CONSTRAINT uq_objet_nom  UNIQUE (nom),
                       CONSTRAINT chk_objet_prix     CHECK (prix >= 0),
                       CONSTRAINT chk_objet_nb_achats CHECK (nb_achats >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/**
 * Table des résumés publiés par les utilisateurs.
 * <p>
 * Chaque résumé est associé à un auteur ({@code uid}) et un cours ({@code code_cours}).
 * L'attribut dérivé {@code note_moyenne} est maintenu automatiquement par trigger.
 * </p>
 */
CREATE TABLE resume (
                        rid              INT          NOT NULL AUTO_INCREMENT,
                        titre            VARCHAR(200) NOT NULL,
                        description      TEXT         NOT NULL,
                        date_publication DATE         NOT NULL,
                        version          INT          NOT NULL DEFAULT 1,
                        visibilite       ENUM('public','prive') NOT NULL DEFAULT 'public',
                        note_moyenne     DECIMAL(3,2) NULL,
                        uid              INT          NOT NULL,
                        code_cours       VARCHAR(20)  NOT NULL,

                        CONSTRAINT pk_resume          PRIMARY KEY (rid),
                        CONSTRAINT fk_resume_auteur   FOREIGN KEY (uid)        REFERENCES utilisateur(uid) ON DELETE CASCADE,
                        CONSTRAINT fk_resume_cours    FOREIGN KEY (code_cours) REFERENCES cours(code)      ON DELETE RESTRICT,
                        CONSTRAINT chk_resume_version CHECK (version >= 1),
                        CONSTRAINT chk_resume_note    CHECK (note_moyenne IS NULL OR (note_moyenne >= 0.0 AND note_moyenne <= 5.0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/**
 * Table des évaluations (notes et commentaires) des résumés.
 * <p>
 * Un utilisateur ne peut évaluer qu'une seule fois chaque résumé
 * (contrainte UNIQUE sur {@code uid, rid}).
 * </p>
 */
CREATE TABLE evaluation (
                            eid         INT            NOT NULL AUTO_INCREMENT,
                            note        DECIMAL(2,1)   NOT NULL,
                            commentaire TEXT,
                            date_eval   DATE           NOT NULL,
                            uid         INT            NOT NULL,
                            rid         INT            NOT NULL,

                            CONSTRAINT pk_evaluation       PRIMARY KEY (eid),
                            CONSTRAINT uq_evaluation       UNIQUE (uid, rid),
                            CONSTRAINT fk_eval_evaluateur  FOREIGN KEY (uid) REFERENCES utilisateur(uid) ON DELETE CASCADE,
                            CONSTRAINT fk_eval_resume      FOREIGN KEY (rid) REFERENCES resume(rid)      ON DELETE CASCADE,
                            CONSTRAINT chk_eval_note       CHECK (note >= 0.0 AND note <= 5.0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/**
 * Table des possessions (objets achetés par les utilisateurs).
 * <p>
 * Clé primaire composite ({@code uid, oid}).
 * Le champ {@code est_actif} indique si l'objet est actuellement équipé.
 * </p>
 */
CREATE TABLE possession (
                            uid         INT     NOT NULL,
                            oid         INT     NOT NULL,
                            date_achat  DATE    NOT NULL,
                            est_actif   BOOLEAN NOT NULL DEFAULT FALSE,

                            CONSTRAINT pk_possession        PRIMARY KEY (uid, oid),
                            CONSTRAINT fk_possession_user   FOREIGN KEY (uid) REFERENCES utilisateur(uid) ON DELETE CASCADE,
                            CONSTRAINT fk_possession_objet  FOREIGN KEY (oid) REFERENCES objet(oid)       ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/**
 * Table des classements périodiques (mensuels ou annuels).
 * <p>
 * Contrainte UNIQUE sur ({@code periode, type_periode}).
 * </p>
 */
CREATE TABLE classement (
                            cid         INT         NOT NULL AUTO_INCREMENT,
                            periode     VARCHAR(20) NOT NULL,
                            type_periode ENUM('mensuel','annuel') NOT NULL,

                            CONSTRAINT pk_classement    PRIMARY KEY (cid),
                            CONSTRAINT uq_classement    UNIQUE (periode, type_periode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/**
 * Table d'association entre utilisateurs et classements.
 * <p>
 * Clé primaire composite ({@code uid, cid}).
 * Stocke la position et les points de l'utilisateur au moment du classement.
 * </p>
 */
CREATE TABLE utilisateur_classement (
                                        uid             INT NOT NULL,
                                        cid             INT NOT NULL,
                                        place           INT NOT NULL,
                                        points_au_moment INT NOT NULL,

                                        CONSTRAINT pk_utilisateur_classement  PRIMARY KEY (uid, cid),
                                        CONSTRAINT fk_uc_utilisateur          FOREIGN KEY (uid) REFERENCES utilisateur(uid) ON DELETE CASCADE,
                                        CONSTRAINT fk_uc_classement           FOREIGN KEY (cid) REFERENCES classement(cid)  ON DELETE CASCADE,
                                        CONSTRAINT chk_uc_place               CHECK (place >= 1),
                                        CONSTRAINT chk_uc_points              CHECK (points_au_moment >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/**
 * Table des transactions de points.
 * <p>
 * Types possibles : 'Gain Publication', 'Gain Évaluation', 'Dépense'.
 * Exactement une des références ({@code rid}, {@code eid}, {@code oid}) est NOT NULL par transaction.
 * La FK composite ({@code utilisateur_uid, oid}) référence possession pour les dépenses.
 * </p>
 */
CREATE TABLE transaction_ (
                              tid               INT         NOT NULL AUTO_INCREMENT,
                              type_transaction  VARCHAR(20) NOT NULL,
                              date_transaction  DATE        NOT NULL,
                              montant           INT         NOT NULL,
                              utilisateur_uid   INT         NOT NULL,
                              rid               INT         NULL,
                              eid               INT         NULL,
                              oid               INT         NULL,

                              CONSTRAINT pk_transaction       PRIMARY KEY (tid),
                              CONSTRAINT fk_trans_user        FOREIGN KEY (utilisateur_uid) REFERENCES utilisateur(uid) ON DELETE RESTRICT,
                              CONSTRAINT fk_trans_resume      FOREIGN KEY (rid)             REFERENCES resume(rid)      ON DELETE RESTRICT,
                              CONSTRAINT fk_trans_eval        FOREIGN KEY (eid)             REFERENCES evaluation(eid)  ON DELETE RESTRICT,
                              CONSTRAINT fk_trans_objet       FOREIGN KEY (oid)             REFERENCES objet(oid)       ON DELETE RESTRICT,
                              CONSTRAINT chk_trans_montant    CHECK (montant <> 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/**
 * Triggers pour le maintien automatique des attributs dérivés.
 * <p>
 * Chaque attribut dérivé est recalculé via des triggers AFTER INSERT/UPDATE/DELETE
 * sur la table source correspondante, garantissant la cohérence des données.
 * </p>
 */

/**
 * Triggers : maintien de {@code Cours.nb_resumes}.
 * <p>
 * Incrémente/décrémente le compteur lors de l'insertion/suppression d'un résumé.
 * </p>
 */
DROP TRIGGER IF EXISTS trg_resume_ins_nb;
DROP TRIGGER IF EXISTS trg_resume_del_nb;

CREATE TRIGGER trg_resume_ins_nb
    AFTER INSERT ON resume
    FOR EACH ROW
    UPDATE cours SET nb_resumes = nb_resumes + 1 WHERE code = NEW.code_cours;

CREATE TRIGGER trg_resume_del_nb
    AFTER DELETE ON resume
    FOR EACH ROW
    UPDATE cours SET nb_resumes = nb_resumes - 1 WHERE code = OLD.code_cours;

/**
 * Triggers : maintien de {@code Objet.nb_achats}.
 * <p>
 * Incrémente/décrémente le compteur lors de l'insertion/suppression d'une possession.
 * </p>
 */
DROP TRIGGER IF EXISTS trg_possession_ins_nb;
DROP TRIGGER IF EXISTS trg_possession_del_nb;

CREATE TRIGGER trg_possession_ins_nb
    AFTER INSERT ON possession
    FOR EACH ROW
    UPDATE objet SET nb_achats = nb_achats + 1 WHERE oid = NEW.oid;

CREATE TRIGGER trg_possession_del_nb
    AFTER DELETE ON possession
    FOR EACH ROW
    UPDATE objet SET nb_achats = nb_achats - 1 WHERE oid = OLD.oid;

/**
 * Triggers : maintien de {@code Utilisateur.total_depense}.
 * <p>
 * Met à jour le total des dépenses lors de l'insertion, suppression ou modification
 * d'une transaction à montant négatif. Utilise une seule instruction UPDATE sans BEGIN/END
 * pour rester compatible avec Spring ScriptUtils.
 * </p>
 */
DROP TRIGGER IF EXISTS trg_trans_ins_depense;
DROP TRIGGER IF EXISTS trg_trans_del_depense;
DROP TRIGGER IF EXISTS trg_trans_upd_depense;

CREATE TRIGGER trg_trans_ins_depense
    AFTER INSERT ON transaction_
    FOR EACH ROW
    UPDATE utilisateur
    SET total_depense = total_depense + ABS(NEW.montant)
    WHERE uid = NEW.utilisateur_uid AND NEW.montant < 0;

CREATE TRIGGER trg_trans_del_depense
    AFTER DELETE ON transaction_
    FOR EACH ROW
    UPDATE utilisateur
    SET total_depense = total_depense - ABS(OLD.montant)
    WHERE uid = OLD.utilisateur_uid AND OLD.montant < 0;

CREATE TRIGGER trg_trans_upd_depense
    AFTER UPDATE ON transaction_
    FOR EACH ROW
    UPDATE utilisateur
    SET total_depense = total_depense
                            - CASE WHEN OLD.montant < 0 THEN ABS(OLD.montant) ELSE 0 END
        + CASE WHEN NEW.montant < 0 THEN ABS(NEW.montant) ELSE 0 END
    WHERE uid = NEW.utilisateur_uid;

/**
 * Triggers : maintien de {@code Resume.note_moyenne}.
 * <p>
 * Recalcule la moyenne des notes d'un résumé lors de l'insertion,
 * suppression ou modification d'une évaluation.
 * </p>
 */
DROP TRIGGER IF EXISTS trg_eval_ins_note;
DROP TRIGGER IF EXISTS trg_eval_del_note;
DROP TRIGGER IF EXISTS trg_eval_upd_note;

CREATE TRIGGER trg_eval_ins_note
    AFTER INSERT ON evaluation
    FOR EACH ROW
    UPDATE resume
    SET note_moyenne = (SELECT AVG(note) FROM evaluation WHERE rid = NEW.rid)
    WHERE rid = NEW.rid;

CREATE TRIGGER trg_eval_del_note
    AFTER DELETE ON evaluation
    FOR EACH ROW
    UPDATE resume
    SET note_moyenne = (SELECT AVG(note) FROM evaluation WHERE rid = OLD.rid)
    WHERE rid = OLD.rid;

CREATE TRIGGER trg_eval_upd_note
    AFTER UPDATE ON evaluation
    FOR EACH ROW
    UPDATE resume
    SET note_moyenne = (SELECT AVG(note) FROM evaluation WHERE rid = NEW.rid)
    WHERE rid = NEW.rid;