-- ============================================================
-- INFOH303 - Projet BDD
-- schema.sql — aligné avec les modèles Java (JPA/Hibernate)
-- MySQL 8 / Spring Boot 3.3
-- ============================================================

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

-- ============================================================
-- TABLE COURS
-- ============================================================

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

-- ============================================================
-- TABLE UTILISATEUR
-- ============================================================

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

-- ============================================================
-- TABLE SEUIL_NIVEAU
-- points_min est la PK (correspond à @Id sur pointsMin dans SeuilNiveau.java)
-- ============================================================

CREATE TABLE seuil_niveau (
                              points_min  INT NOT NULL,
                              niveau      INT NOT NULL,

                              CONSTRAINT pk_seuil_niveau   PRIMARY KEY (points_min),
                              CONSTRAINT uq_seuil_niveau   UNIQUE (niveau),
                              CONSTRAINT chk_seuil_points  CHECK (points_min >= 0),
                              CONSTRAINT chk_seuil_niveau  CHECK (niveau >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- TABLE OBJET
-- type ENUM aligné sur TypeObjet.java : badge | titre | theme | cosmetique
-- ============================================================

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

-- ============================================================
-- TABLE RESUME
-- visibilite ENUM alignée sur le @Pattern du modèle Java
-- uid -> utilisateur (auteur), code_cours -> cours
-- ============================================================

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

-- ============================================================
-- TABLE EVALUATION
-- note DECIMAL(2,1) alignée sur BigDecimal dans Evaluation.java
-- UNIQUE(uid, rid) : un utilisateur ne peut évaluer qu'une fois chaque résumé
-- ============================================================

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

-- ============================================================
-- TABLE POSSESSION
-- PK composite (uid, oid) — correspond à @IdClass(PossessionId.class)
-- est_actif correspond au champ estActif dans Possession.java
-- ============================================================

CREATE TABLE possession (
                            uid         INT     NOT NULL,
                            oid         INT     NOT NULL,
                            date_achat  DATE    NOT NULL,
                            est_actif   BOOLEAN NOT NULL DEFAULT FALSE,

                            CONSTRAINT pk_possession        PRIMARY KEY (uid, oid),
                            CONSTRAINT fk_possession_user   FOREIGN KEY (uid) REFERENCES utilisateur(uid) ON DELETE CASCADE,
                            CONSTRAINT fk_possession_objet  FOREIGN KEY (oid) REFERENCES objet(oid)       ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- TABLE CLASSEMENT
-- type_periode ENUM aligné sur TypePeriode.java : mensuel | annuel
-- UNIQUE(periode, type_periode)
-- ============================================================

CREATE TABLE classement (
                            cid         INT         NOT NULL AUTO_INCREMENT,
                            periode     VARCHAR(20) NOT NULL,
                            type_periode ENUM('mensuel','annuel') NOT NULL,

                            CONSTRAINT pk_classement    PRIMARY KEY (cid),
                            CONSTRAINT uq_classement    UNIQUE (periode, type_periode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- TABLE UTILISATEUR_CLASSEMENT
-- PK composite (uid, cid) — correspond à @IdClass(UtilisateurClassementId.class)
-- ============================================================

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

-- ============================================================
-- TABLE TRANSACTION_
-- type_transaction VARCHAR aligné sur TypeTransactionConverter.java
--   valeurs : 'Gain Publication' | 'Gain Évaluation' | 'Dépense'
-- FK composite (utilisateur_uid, oid) -> possession(uid, oid) pour les Dépenses
-- ============================================================

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
-- ============================================================
-- TRIGGERS — Maintien des attributs dérivés
-- ============================================================

-- --------------------------------------------------------
-- Trigger : Cours.nb_resumes
-- --------------------------------------------------------
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

-- --------------------------------------------------------
-- Trigger : Objet.nb_achats
-- --------------------------------------------------------
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

-- --------------------------------------------------------
-- Trigger : Utilisateur.total_depense
-- --------------------------------------------------------
DROP TRIGGER IF EXISTS trg_trans_ins_depense;
DROP TRIGGER IF EXISTS trg_trans_del_depense;
DROP TRIGGER IF EXISTS trg_trans_upd_depense;

-- Sans BEGIN/END : le IF est remplacé par un WHERE conditionnel,
-- ce qui évite le problème de parsing du ; par Spring ScriptUtils.
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

-- Pour UPDATE : on corrige la dépense en recalculant le delta via CASE.
-- Toujours une seule instruction, compatible Spring ScriptUtils.
CREATE TRIGGER trg_trans_upd_depense
    AFTER UPDATE ON transaction_
    FOR EACH ROW
    UPDATE utilisateur
    SET total_depense = total_depense
                            - CASE WHEN OLD.montant < 0 THEN ABS(OLD.montant) ELSE 0 END
        + CASE WHEN NEW.montant < 0 THEN ABS(NEW.montant) ELSE 0 END
    WHERE uid = NEW.utilisateur_uid;

-- --------------------------------------------------------
-- Trigger : Resume.note_moyenne
-- --------------------------------------------------------
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
