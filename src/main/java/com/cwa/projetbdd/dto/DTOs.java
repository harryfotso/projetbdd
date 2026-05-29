package com.cwa.projetbdd.dto;

import com.cwa.projetbdd.models.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Tous les DTOs (objets de transfert) utilises par les controllers.
 * Permet d'eviter les problemes de lazy-loading et controle ce qui sort en JSON.
 */
public class DTOs {

    // ========== AUTH ==========
    @Data @NoArgsConstructor @AllArgsConstructor/** @Data evite d'ecrire pour spring les guetter
    et les setter ainsi que d'autres methodes */

    /**  @NoArgsConstructor important pour l'orseque l'on apelle le constructeur avec un @RequestBody
     * celui ci crée un objet vide qu'il vas completer petit a petit */

    /**C'est utile quand tu connais déjà toutes les valeurs au moment de créer l'objet
     */
     public static class LoginRequest {
        private String nom;
        private String motDePasse;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class RegisterRequest {
        private String nom;
        private String email;
        private String motDePasse;
    }

    /**
     * DTO pour le transfert de données des utilisateurs.
     */
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class UtilisateurDTO {
        private Integer uid;
        private String nom;
        private String email;
        private LocalDate dateInscription;
        private Integer points;
        private Integer niveau;
        private ObjetDTO badgeActif;
        private ObjetDTO titreActif;

        /**
         * Construction basique depuis l'entite (sans niveau ni actifs).
         * Le service enrichira ensuite niveau, badgeActif, titreActif.
         */
        public static UtilisateurDTO from(Utilisateur u) {
            if (u == null) return null;
            return UtilisateurDTO.builder()
                    .uid(u.getUid())
                    .nom(u.getNom())
                    .email(u.getEmail())
                    .dateInscription(u.getDateInscription())
                    .points(u.getPoints())
                    .build();
        }
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class UtilisateurUpdateRequest {
        private String nom;
        private String email;
        private Integer badgeActif;
        private Integer titreActif;
    }

    /**
     * DTO pour le transfert de données des cours.
     */
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CoursDTO {
        private String code;
        private String nom;
        private String faculte;
        private Integer credits;
        private String anneeAcademique;
        private Long nbResumes;

        public static CoursDTO from(Cours c) {
            if (c == null) return null;
            return CoursDTO.builder()
                    .code(c.getCode()).nom(c.getNom()).faculte(c.getFaculte())
                    .credits(c.getCredits()).anneeAcademique(c.getAnneeAcademique())
                    .build();
        }
    }

    /**
     * DTO pour le transfert de données des résumés.
     */
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ResumeDTO {
        private Integer rid;
        private String titre;
        private String description;
        private LocalDate datePublication;
        private Integer version;
        private String visibilite;
        private Integer auteurUid;
        private String auteurNom;
        private String codeCours;
        private String coursNom;
        private BigDecimal noteMoyenne;
        private Integer nbEvaluations;

        public static ResumeDTO from(Resume r) {
            if (r == null) return null;
            return ResumeDTO.builder()
                    .rid(r.getRid()).titre(r.getTitre()).description(r.getDescription())
                    .datePublication(r.getDatePublication()).version(r.getVersion())
                    .visibilite(r.getVisibilite())
                    .auteurUid(r.getAuteur() != null ? r.getAuteur().getUid() : null)
                    .auteurNom(r.getAuteur() != null ? r.getAuteur().getNom() : null)
                    .codeCours(r.getCours() != null ? r.getCours().getCode() : null)
                    .coursNom(r.getCours() != null ? r.getCours().getNom() : null)
                    .build();
        }
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ResumeRequest {
        private String titre;
        private String description;
        private String visibilite;
        private String codeCours;
    }

    // ========== EVALUATION ==========
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class EvaluationDTO {
        private Integer eid;
        private BigDecimal note;
        private String commentaire;
        private LocalDate dateEval;
        private Integer evaluateurUid;
        private String evaluateurNom;
        private Integer rid;
        private String resumeTitre;

        public static EvaluationDTO from(Evaluation e) {
            if (e == null) return null;
            return EvaluationDTO.builder()
                    .eid(e.getEid()).note(e.getNote()).commentaire(e.getCommentaire())
                    .dateEval(e.getDateEval())
                    .evaluateurUid(e.getEvaluateur() != null ? e.getEvaluateur().getUid() : null)
                    .evaluateurNom(e.getEvaluateur() != null ? e.getEvaluateur().getNom() : null)
                    .rid(e.getResume() != null ? e.getResume().getRid() : null)
                    .resumeTitre(e.getResume() != null ? e.getResume().getTitre() : null)
                    .build();
        }
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class EvaluationRequest {
        private Integer rid;
        private BigDecimal note;
        private String commentaire;
    }

    /**
     * DTO pour le transfert de données des objets.
     */
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ObjetDTO {
        private Integer oid;
        private String nom;
        private String description;
        private Integer prix;
        private TypeObjet type;

        public static ObjetDTO from(Objet o) {
            if (o == null) return null;
            return ObjetDTO.builder()
                    .oid(o.getOid()).nom(o.getNom()).description(o.getDescription())
                    .prix(o.getPrix()).type(o.getType()).build();
        }
    }

    /**
     * DTO pour le transfert de données des possessions.
     */
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PossessionDTO {
        private Integer uid;
        private Integer oid;
        private LocalDate dateAchat;
        private Boolean estActif;
        private ObjetDTO objet;

        public static PossessionDTO from(Possession p) {
            if (p == null) return null;
            return PossessionDTO.builder()
                    .uid(p.getUtilisateur().getUid())
                    .oid(p.getObjet().getOid())
                    .dateAchat(p.getDateAchat())
                    .estActif(p.getEstActif())
                    .objet(ObjetDTO.from(p.getObjet()))
                    .build();
        }
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class AchatRequest {
        private Integer uid;
        private Integer oid;
    }

    /**
     * DTO pour le transfert de données des transactions.
     */
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TransactionDTO {
        private Integer tid;
        private String typeTransaction;
        private LocalDate dateTransaction;
        private Integer montant;
        private Integer utilisateurUid;
        private String description;  // libelle lisible

        public static TransactionDTO from(Transaction t) {
            if (t == null) return null;
            String desc = switch (t.getTypeTransaction()) {
                case GAIN_PUBLICATION -> "Publication : " +
                        (t.getResume() != null ? t.getResume().getTitre() : "?");
                case GAIN_EVALUATION -> "Evaluation recue : " +
                        (t.getEvaluation() != null && t.getEvaluation().getResume() != null
                                ? t.getEvaluation().getResume().getTitre() : "?");
                case DEPENSE -> "Achat : " +
                        (t.getObjet() != null ? t.getObjet().getNom() : "?");
            };
            return TransactionDTO.builder()
                    .tid(t.getTid()).typeTransaction(t.getTypeTransaction().getLabel())
                    .dateTransaction(t.getDateTransaction()).montant(t.getMontant())
                    .utilisateurUid(t.getUtilisateur().getUid()).description(desc)
                    .build();
        }
    }

    // ========== LEADERBOARD ==========
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LeaderboardEntry {
        private Integer rang;
        private Integer uid;
        private String nom;
        private Integer points;
        private Integer niveau;
        private Long nbResumes;
        private String titreActif;
        private String badgeActif;
    }

    // ========== ANALYTICS RESULTS ==========
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CoursPlusResumes {
        private String code;
        private String nom;
        private Long nbResumes;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MeilleurResumeParCours {
        private String codeCours;
        private Integer rid;
        private String titre;
        private BigDecimal noteMoyenne;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ObjetPlusAchete {
        private Integer oid;
        private String nom;
        private TypeObjet type;
        private Long nbAchats;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Overspender {
        private Integer uid;
        private String nom;
        private Integer pointsActuels;
        private Long totalDepense;
    }

    // ========== GENERIC ==========
    @Data @AllArgsConstructor
    public static class MessageResponse {
        private String message;
    }

    @Data @AllArgsConstructor
    public static class CountResponse {
        private Long count;
    }

    @Data @AllArgsConstructor
    public static class StatsDashboard {
        private Integer points;
        private Integer niveau;
        private Long nbResumes;
        private Double noteMoyenneRecue;
    }
}
