package com.cwa.projetbdd.init;

import com.cwa.projetbdd.models.*;
import com.cwa.projetbdd.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Initialisation de la base de donnees a partir des 4 fichiers sources :
 *   - data/cours.csv          (CSV)   -> table cours
 *   - data/recompenses.xml    (XML)   -> table objet
 *   - data/utilisateurs.xml   (XML)   -> tables utilisateur, resume, possession
 *   - data/commentaires.json  (JSON)  -> table evaluation
 *
 * Strategie (choix valides avec l'utilisateur) :
 *   - Reference introuvable        -> on IGNORE l'enregistrement + log d'avertissement
 *   - Mot de passe absent          -> le hash est derive du nom d'utilisateur
 *   - Frequence                    -> import UNIQUEMENT si la base est vide
 *
 * Robustesse : chaque enregistrement est traite dans son propre try/catch,
 * donc une donnee corrompue n'interrompt jamais l'import global.
 * Un rapport de synthese est affiche a la fin.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final CoursRepository coursRepository;
    private final ObjetRepository objetRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ResumeRepository resumeRepository;
    private final EvaluationRepository evaluationRepository;
    private final PossessionRepository possessionRepository;

    /** Compteur simple pour le rapport final. */
    private static class Rapport {
        int importes = 0;
        int ignores = 0;
        void ok() { importes++; }
        void skip() { ignores++; }
        @Override public String toString() {
            return importes + " importes, " + ignores + " ignores";
        }
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Garde-fou : on n'importe que si la base est vide.
        if (utilisateurRepository.count() > 0 || coursRepository.count() > 0) {
            log.info("[INIT] La base contient deja des donnees — import ignore.");
            return;
        }

        log.info("[INIT] ===== Debut de l'import des donnees =====");

        // L'ordre est important : on cree d'abord ce qui est reference par d'autres.
        importerCours();        // 1. les cours (references par les resumes)
        importerObjets();       // 2. les objets (references par les achats)
        importerUtilisateurs(); // 3. les utilisateurs + leurs resumes + leurs achats
        importerEvaluations();  // 4. les evaluations (referencent utilisateurs + resumes)

        log.info("[INIT] ===== Import termine =====");
    }

    // ============================================================
    // 1. COURS  (CSV)
    // Format : code_cours,nom,faculte,credits
    // ============================================================
    private void importerCours() {
        Rapport r = new Rapport();
        try (BufferedReader br = lire("data/cours.csv")) {
            String ligne = br.readLine(); // saute l'en-tete
            int numLigne = 1;
            while ((ligne = br.readLine()) != null) {
                numLigne++;
                if (ligne.isBlank()) continue;
                try {
                    String[] champs = ligne.split(",");
                    if (champs.length < 4) {
                        log.warn("[INIT][cours] ligne {} malformee (moins de 4 colonnes) : {}", numLigne, ligne);
                        r.skip();
                        continue;
                    }
                    Cours cours = Cours.builder()
                            .code(champs[0].trim())
                            .nom(champs[1].trim())
                            .faculte(champs[2].trim())
                            .credits(Integer.parseInt(champs[3].trim()))
                            .anneeAcademique("2025-2026")
                            .nbResumes(0)
                            .build();
                    coursRepository.save(cours);
                    r.ok();
                } catch (Exception e) {
                    log.warn("[INIT][cours] ligne {} ignoree : {}", numLigne, e.getMessage());
                    r.skip();
                }
            }
        } catch (Exception e) {
            log.error("[INIT][cours] Impossible de lire cours.csv : {}", e.getMessage());
        }
        log.info("[INIT][cours] {}", r);
    }

    // ============================================================
    // 2. OBJETS  (XML)
    // Format : <objet id="1"><nom/><type/><description/><prix/></objet>
    // ============================================================
    private void importerObjets() {
        Rapport r = new Rapport();
        try {
            Document doc = parserXml("data/recompenses.xml");
            NodeList objets = doc.getElementsByTagName("objet");
            for (int i = 0; i < objets.getLength(); i++) {
                Element el = (Element) objets.item(i);
                try {
                    String nom = texte(el, "nom");
                    String typeStr = texte(el, "type");
                    String description = texte(el, "description");
                    String prixStr = texte(el, "prix");

                    if (nom == null || typeStr == null) {
                        log.warn("[INIT][objet] objet sans nom ou type — ignore");
                        r.skip();
                        continue;
                    }
                    // L'enum TypeObjet vaut : badge | titre | theme | cosmetique
                    TypeObjet type = TypeObjet.valueOf(typeStr.trim().toLowerCase());

                    Objet objet = Objet.builder()
                            .nom(nom.trim())
                            .type(type)
                            .description(description != null ? description.trim() : null)
                            .prix(prixStr != null ? Integer.parseInt(prixStr.trim()) : 0)
                            .nbAchats(0)
                            .build();
                    objetRepository.save(objet);
                    r.ok();
                } catch (IllegalArgumentException e) {
                    log.warn("[INIT][objet] type inconnu ou prix invalide — objet ignore : {}", e.getMessage());
                    r.skip();
                } catch (Exception e) {
                    log.warn("[INIT][objet] objet ignore : {}", e.getMessage());
                    r.skip();
                }
            }
        } catch (Exception e) {
            log.error("[INIT][objet] Impossible de lire recompenses.xml : {}", e.getMessage());
        }
        log.info("[INIT][objet] {}", r);
    }

    // ============================================================
    // 3. UTILISATEURS  (XML)
    // Chaque <utilisateur> contient aussi ses <resumes> et ses <achats>.
    // ============================================================
    private void importerUtilisateurs() {
        Rapport rUsers = new Rapport();
        Rapport rResumes = new Rapport();
        Rapport rAchats = new Rapport();

        try {
            Document doc = parserXml("data/utilisateurs.xml");
            NodeList users = doc.getElementsByTagName("utilisateur");

            for (int i = 0; i < users.getLength(); i++) {
                Element el = (Element) users.item(i);
                Utilisateur sauvegarde;
                try {
                    String nom = texte(el, "nomUtilisateur");
                    String email = texte(el, "email");
                    String dateStr = texte(el, "dateInscription");
                    String pointsStr = texte(el, "points");

                    if (nom == null || email == null) {
                        log.warn("[INIT][user] utilisateur sans nom/email — ignore");
                        rUsers.skip();
                        continue;
                    }

                    Utilisateur u = Utilisateur.builder()
                            .nom(nom.trim())
                            .email(email.trim())
                            // mot de passe = nom d'utilisateur (choix valide)
                            .motDePasseHash(simpleHash(nom.trim()))
                            .dateInscription(dateStr != null ? LocalDate.parse(dateStr.trim()) : LocalDate.now())
                            .points(pointsStr != null ? Integer.parseInt(pointsStr.trim()) : 0)
                            .totalDepense(0)
                            .build();
                    sauvegarde = utilisateurRepository.save(u);
                    rUsers.ok();
                } catch (Exception e) {
                    log.warn("[INIT][user] utilisateur ignore : {}", e.getMessage());
                    rUsers.skip();
                    continue; // sans utilisateur, pas de resume ni d'achat a traiter
                }

                // --- Resumes de cet utilisateur ---
                Element resumesEl = premierEnfant(el, "resumes");
                if (resumesEl != null) {
                    NodeList resumes = resumesEl.getElementsByTagName("resume");
                    for (int j = 0; j < resumes.getLength(); j++) {
                        Element rEl = (Element) resumes.item(j);
                        try {
                            String codeCours = texte(rEl, "cours");
                            String titre = texte(rEl, "titre");
                            String datePub = texte(rEl, "datePublication");
                            String noteMoy = texte(rEl, "noteMoyenne");

                            Optional<Cours> cours = (codeCours != null)
                                    ? coursRepository.findById(codeCours.trim())
                                    : Optional.empty();
                            if (cours.isEmpty()) {
                                log.warn("[INIT][resume] cours '{}' introuvable — resume '{}' ignore",
                                        codeCours, titre);
                                rResumes.skip();
                                continue;
                            }

                            Resume resume = Resume.builder()
                                    .titre(titre != null ? titre.trim() : "(sans titre)")
                                    .description(titre != null ? titre.trim() : "(import)")
                                    .datePublication(datePub != null ? LocalDate.parse(datePub.trim()) : LocalDate.now())
                                    .version(1)
                                    .visibilite("public")
                                    .auteur(sauvegarde)
                                    .cours(cours.get())
                                    .noteMoyenne(noteMoy != null ? new BigDecimal(noteMoy.trim()) : null)
                                    .build();
                            resumeRepository.save(resume);
                            rResumes.ok();
                        } catch (Exception e) {
                            log.warn("[INIT][resume] resume ignore : {}", e.getMessage());
                            rResumes.skip();
                        }
                    }
                }

                // --- Achats (objets possedes) de cet utilisateur ---
                Element achatsEl = premierEnfant(el, "achats");
                if (achatsEl != null) {
                    NodeList objs = achatsEl.getElementsByTagName("objet");
                    for (int j = 0; j < objs.getLength(); j++) {
                        String nomObjet = objs.item(j).getTextContent();
                        try {
                            if (nomObjet == null || nomObjet.isBlank()) { rAchats.skip(); continue; }
                            Optional<Objet> objet = objetRepository.findByNom(nomObjet.trim());
                            if (objet.isEmpty()) {
                                log.warn("[INIT][achat] objet '{}' introuvable — achat ignore", nomObjet.trim());
                                rAchats.skip();
                                continue;
                            }
                            Possession p = Possession.builder()
                                    .utilisateur(sauvegarde)
                                    .objet(objet.get())
                                    .dateAchat(LocalDate.now())
                                    .estActif(false)
                                    .build();
                            possessionRepository.save(p);
                            rAchats.ok();
                        } catch (Exception e) {
                            log.warn("[INIT][achat] achat '{}' ignore : {}", nomObjet, e.getMessage());
                            rAchats.skip();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("[INIT][user] Impossible de lire utilisateurs.xml : {}", e.getMessage());
        }
        log.info("[INIT][user] utilisateurs : {}", rUsers);
        log.info("[INIT][user] resumes      : {}", rResumes);
        log.info("[INIT][user] achats       : {}", rAchats);
    }

    // ============================================================
    // 4. EVALUATIONS  (JSON)
    // Format : { "evaluations": [ { auteur, destinataire, resume:{cours,titre}, note, commentaire } ] }
    // On relie l'evaluation a son resume via (cours + titre) et a son auteur via le nom.
    // ============================================================
    private void importerEvaluations() {
        Rapport r = new Rapport();
        try (InputStream in = new ClassPathResource("data/commentaires.json").getInputStream()) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode racine = mapper.readTree(in);
            JsonNode evals = racine.get("evaluations");
            if (evals == null || !evals.isArray()) {
                log.error("[INIT][eval] champ 'evaluations' absent ou invalide dans commentaires.json");
                return;
            }

            for (JsonNode e : evals) {
                try {
                    String auteur = texteJson(e, "auteur");
                    JsonNode resNode = e.get("resume");
                    String coursCode = resNode != null ? texteJson(resNode, "cours") : null;
                    String titre = resNode != null ? texteJson(resNode, "titre") : null;
                    String noteStr = texteJson(e, "note");
                    String commentaire = texteJson(e, "commentaire");

                    // L'evaluateur (auteur) doit exister
                    Optional<Utilisateur> evaluateur = (auteur != null)
                            ? utilisateurRepository.findByNom(auteur.trim())
                            : Optional.empty();
                    if (evaluateur.isEmpty()) {
                        log.warn("[INIT][eval] auteur '{}' introuvable — evaluation ignoree", auteur);
                        r.skip();
                        continue;
                    }

                    // Le resume est identifie par (cours + titre)
                    Resume resume = trouverResume(coursCode, titre);
                    if (resume == null) {
                        log.warn("[INIT][eval] resume (cours='{}', titre='{}') introuvable — evaluation ignoree",
                                coursCode, titre);
                        r.skip();
                        continue;
                    }

                    Evaluation eval = Evaluation.builder()
                            .note(noteStr != null ? new BigDecimal(noteStr.trim()) : BigDecimal.ZERO)
                            .commentaire(commentaire)
                            .dateEval(LocalDate.now())
                            .evaluateur(evaluateur.get())
                            .resume(resume)
                            .build();
                    evaluationRepository.save(eval);
                    r.ok();
                } catch (Exception ex) {
                    log.warn("[INIT][eval] evaluation ignoree : {}", ex.getMessage());
                    r.skip();
                }
            }
        } catch (Exception e) {
            log.error("[INIT][eval] Impossible de lire commentaires.json : {}", e.getMessage());
        }
        log.info("[INIT][eval] {}", r);
    }

    /** Cherche un resume par code de cours et titre (best effort). */
    private Resume trouverResume(String coursCode, String titre) {
        if (coursCode == null || titre == null) return null;
        return resumeRepository.findByCoursCode(coursCode.trim()).stream()
                .filter(res -> titre.trim().equalsIgnoreCase(res.getTitre()))
                .findFirst()
                .orElse(null);
    }

    // ============================================================
    // Utilitaires
    // ============================================================

    private BufferedReader lire(String chemin) throws Exception {
        InputStream in = new ClassPathResource(chemin).getInputStream();
        return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    private Document parserXml(String chemin) throws Exception {
        try (InputStream in = new ClassPathResource(chemin).getInputStream()) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            Document doc = factory.newDocumentBuilder().parse(in);
            doc.getDocumentElement().normalize();
            return doc;
        }
    }

    /** Renvoie le texte du premier sous-element <balise> direct ou descendant, ou null. */
    private String texte(Element parent, String balise) {
        NodeList nl = parent.getElementsByTagName(balise);
        if (nl.getLength() == 0) return null;
        // On se limite au premier element rencontre.
        Node n = nl.item(0);
        return n != null ? n.getTextContent() : null;
    }

    /** Renvoie le premier sous-element direct portant ce nom de balise, ou null. */
    private Element premierEnfant(Element parent, String balise) {
        NodeList enfants = parent.getChildNodes();
        for (int i = 0; i < enfants.getLength(); i++) {
            Node n = enfants.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals(balise)) {
                return (Element) n;
            }
        }
        return null;
    }

    private String texteJson(JsonNode node, String champ) {
        JsonNode v = node.get(champ);
        return (v != null && !v.isNull()) ? v.asText() : null;
    }

    /** Meme logique de hash que UtilisateurService, pour rester coherent. */
    private String simpleHash(String input) {
        return "$h$" + Integer.toHexString(input.hashCode());
    }
}
