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
@Slf4j                  // genere automatiquement un logger 'log' (SLF4J) utilisable partout
@Component              // declare la classe comme bean Spring : sera instanciee et geree par le conteneur
@RequiredArgsConstructor // Lombok genere un constructeur avec tous les champs 'final' (injection de dependances)
public class DataInitializer implements ApplicationRunner {
    // ApplicationRunner : interface Spring Boot, sa methode run() est appelee
    // automatiquement une fois que l'application est completement demarree.

    // Repositories injectes par Spring (via le constructeur genere par Lombok)
    // Chaque repository donne acces aux operations CRUD sur une table de la base.
    private final CoursRepository coursRepository;
    private final ObjetRepository objetRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ResumeRepository resumeRepository;
    private final EvaluationRepository evaluationRepository;
    private final PossessionRepository possessionRepository;

    /**
     * Compteur simple pour le rapport final.
     * Petite classe interne utilisee pour suivre, par etape d'import,
     * combien d'enregistrements ont ete importes avec succes vs ignores.
     */
    private static class Rapport {
        int importes = 0;
        int ignores = 0;
        void ok() { importes++; }   // a appeler pour chaque enregistrement importe avec succes
        void skip() { ignores++; }  // a appeler pour chaque enregistrement ignore (erreur, reference manquante...)
        @Override public String toString() {
            return importes + " importes, " + ignores + " ignores";
        }
    }

    /**
     * Point d'entree de l'import : execute automatiquement par Spring Boot
     * au demarrage de l'application.
     *
     * @Transactional : tout l'import s'execute dans une seule transaction ;
     * si une exception non rattrapee remonte, tout est annule (rollback).
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Garde-fou : on n'importe que si la base est vide.
        // Evite de dupliquer les donnees a chaque redemarrage de l'application.
        if (utilisateurRepository.count() > 0 || coursRepository.count() > 0) {
            log.info("[INIT] La base contient deja des donnees — import ignore.");
            return;
        }

        log.info("[INIT] ===== Debut de l'import des donnees =====");

        // L'ordre est important : on cree d'abord ce qui est reference par d'autres
        // (cles etrangeres). Sinon les contraintes FK echoueraient.
        importerCours();        // 1. les cours (references par les resumes)
        importerObjets();       // 2. les objets (references par les achats)
        importerUtilisateurs(); // 3. les utilisateurs + leurs resumes + leurs achats
        importerEvaluations();  // 4. les evaluations (referencent utilisateurs + resumes)

        log.info("[INIT] ===== Import termine =====");
    }

    /**
     * Importe les cours depuis data/cours.csv.
     * Strategie : lecture ligne par ligne, on saute l'en-tete, chaque ligne
     * est traitee dans son propre try/catch pour qu'une ligne mal formee
     * n'interrompe pas l'import des autres.
     */
    private void importerCours() {
        Rapport r = new Rapport();
        try (BufferedReader br = lire("data/cours.csv")) { // try-with-resources : ferme le reader automatiquement
            String ligne = br.readLine(); // saute l'en-tete (premiere ligne du CSV)
            int numLigne = 1;
            while ((ligne = br.readLine()) != null) {
                numLigne++;
                if (ligne.isBlank()) continue; // ignore les lignes vides
                try {
                    String[] champs = ligne.split(",");
                    if (champs.length < 4) {
                        // Ligne incomplete : on ne peut pas construire un Cours valide
                        log.warn("[INIT][cours] ligne {} malformee (moins de 4 colonnes) : {}", numLigne, ligne);
                        r.skip();
                        continue;
                    }
                    // Construction de l'entite Cours avec le builder Lombok
                    Cours cours = Cours.builder()
                            .code(champs[0].trim())
                            .nom(champs[1].trim())
                            .faculte(champs[2].trim())
                            .credits(Integer.parseInt(champs[3].trim()))
                            .anneeAcademique("2025-2026") // valeur fixee pour tous les cours importes
                            .nbResumes(0)                 // compteur derive maintenu par trigger en BDD
                            .build();
                    coursRepository.save(cours); // INSERT en base via JPA
                    r.ok();
                } catch (Exception e) {
                    // Toute erreur sur une ligne (parsing du nombre, etc.) -> on ignore juste cette ligne
                    log.warn("[INIT][cours] ligne {} ignoree : {}", numLigne, e.getMessage());
                    r.skip();
                }
            }
        } catch (Exception e) {
            // Erreur a l'ouverture du fichier : on ne peut rien importer du tout
            log.error("[INIT][cours] Impossible de lire cours.csv : {}", e.getMessage());
        }
        log.info("[INIT][cours] {}", r); // affiche le rapport final pour cette etape
    }

    /**
     * Importe les objets cosmetiques (badges, titres, themes...) depuis recompenses.xml.
     * Utilise l'API DOM Java pour parser le XML : on charge tout en memoire
     * puis on itere sur les noeuds <objet>.
     */
    private void importerObjets() {
        Rapport r = new Rapport();
        try {
            Document doc = parserXml("data/recompenses.xml");
            NodeList objets = doc.getElementsByTagName("objet"); // recupere tous les <objet>
            for (int i = 0; i < objets.getLength(); i++) {
                Element el = (Element) objets.item(i);
                try {
                    // Extraction des champs via la methode utilitaire texte()
                    String nom = texte(el, "nom");
                    String typeStr = texte(el, "type");
                    String description = texte(el, "description");
                    String prixStr = texte(el, "prix");

                    // Champs obligatoires : nom et type
                    if (nom == null || typeStr == null) {
                        log.warn("[INIT][objet] objet sans nom ou type — ignore");
                        r.skip();
                        continue;
                    }
                    // L'enum TypeObjet vaut : badge | titre | theme | cosmetique
                    // valueOf() leve IllegalArgumentException si la valeur n'est pas dans l'enum
                    TypeObjet type = TypeObjet.valueOf(typeStr.trim().toLowerCase());

                    Objet objet = Objet.builder()
                            .nom(nom.trim())
                            .type(type)
                            .description(description != null ? description.trim() : null)
                            .prix(prixStr != null ? Integer.parseInt(prixStr.trim()) : 0)
                            .nbAchats(0) // compteur derive maintenu par trigger
                            .build();
                    objetRepository.save(objet);
                    r.ok();
                } catch (IllegalArgumentException e) {
                    // Cas specifique : type d'objet inconnu ou prix non numerique
                    log.warn("[INIT][objet] type inconnu ou prix invalide — objet ignore : {}", e.getMessage());
                    r.skip();
                } catch (Exception e) {
                    // Toute autre erreur -> on ignore cet objet
                    log.warn("[INIT][objet] objet ignore : {}", e.getMessage());
                    r.skip();
                }
            }
        } catch (Exception e) {
            log.error("[INIT][objet] Impossible de lire recompenses.xml : {}", e.getMessage());
        }
        log.info("[INIT][objet] {}", r);
    }

    /**
     * Etape la plus complexe : pour chaque utilisateur dans le XML,
     * on cree l'utilisateur, PUIS ses resumes (qui referencent un cours),
     * PUIS ses achats (qui referencent un objet).
     * On utilise donc 3 rapports distincts pour suivre chaque sous-import.
     */
    private void importerUtilisateurs() {
        Rapport rUsers = new Rapport();
        Rapport rResumes = new Rapport();
        Rapport rAchats = new Rapport();

        try {
            Document doc = parserXml("data/utilisateurs.xml");
            NodeList users = doc.getElementsByTagName("utilisateur");

            for (int i = 0; i < users.getLength(); i++) {
                Element el = (Element) users.item(i);
                Utilisateur sauvegarde; // reference vers l'utilisateur sauvegarde, reutilise plus bas
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
                            // Mot de passe absent dans la source : on derive un hash a partir du nom (choix valide)
                            .motDePasseHash(simpleHash(nom.trim()))
                            // Si la date est absente, on prend la date du jour comme valeur par defaut
                            .dateInscription(dateStr != null ? LocalDate.parse(dateStr.trim()) : LocalDate.now())
                            .points(pointsStr != null ? Integer.parseInt(pointsStr.trim()) : 0)
                            .totalDepense(0) // attribut derive maintenu par trigger
                            .build();
                    // save() retourne l'entite avec son uid genere par la base : on en a besoin pour les FK plus bas
                    sauvegarde = utilisateurRepository.save(u);
                    rUsers.ok();
                } catch (Exception e) {
                    log.warn("[INIT][user] utilisateur ignore : {}", e.getMessage());
                    rUsers.skip();
                    continue; // sans utilisateur, pas de resume ni d'achat a traiter
                }

                /** Resumes de cet utilisateur */
                // On cherche le sous-element <resumes> puis on itere sur ses <resume> enfants
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

                            // Le resume doit referencer un cours existant : on tente de le retrouver par son code
                            Optional<Cours> cours = (codeCours != null)
                                    ? coursRepository.findById(codeCours.trim())
                                    : Optional.empty();
                            if (cours.isEmpty()) {
                                // Strategie : reference introuvable -> on ignore
                                log.warn("[INIT][resume] cours '{}' introuvable — resume '{}' ignore",
                                        codeCours, titre);
                                rResumes.skip();
                                continue;
                            }

                            Resume resume = Resume.builder()
                                    .titre(titre != null ? titre.trim() : "(sans titre)")
                                    // description manquante dans la source : on reutilise le titre comme fallback
                                    .description(titre != null ? titre.trim() : "(import)")
                                    .datePublication(datePub != null ? LocalDate.parse(datePub.trim()) : LocalDate.now())
                                    .version(1)
                                    .visibilite("public")
                                    .auteur(sauvegarde)      // FK vers l'utilisateur qu'on vient de creer
                                    .cours(cours.get())      // FK vers le cours retrouve
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
                // Idem : on cherche <achats> puis on itere sur ses <objet> enfants.
                // Ici le nom de l'objet est directement dans le texte du noeud <objet>.
                Element achatsEl = premierEnfant(el, "achats");
                if (achatsEl != null) {
                    NodeList objs = achatsEl.getElementsByTagName("objet");
                    for (int j = 0; j < objs.getLength(); j++) {
                        String nomObjet = objs.item(j).getTextContent();
                        try {
                            if (nomObjet == null || nomObjet.isBlank()) { rAchats.skip(); continue; }
                            // On retrouve l'objet par son nom (suppose unique en base)
                            Optional<Objet> objet = objetRepository.findByNom(nomObjet.trim());
                            if (objet.isEmpty()) {
                                log.warn("[INIT][achat] objet '{}' introuvable — achat ignore", nomObjet.trim());
                                rAchats.skip();
                                continue;
                            }
                            // Une Possession represente le lien utilisateur <-> objet (table d'association)
                            Possession p = Possession.builder()
                                    .utilisateur(sauvegarde)
                                    .objet(objet.get())
                                    .dateAchat(LocalDate.now()) // date d'achat non fournie dans la source
                                    .estActif(false)             // objet non equipe par defaut
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
        // Rapport final de l'etape 3 : un log par sous-import
        log.info("[INIT][user] utilisateurs : {}", rUsers);
        log.info("[INIT][user] resumes      : {}", rResumes);
        log.info("[INIT][user] achats       : {}", rAchats);
    }

    /**
     * Importe les evaluations (notes + commentaires) depuis commentaires.json.
     * Utilise la librairie Jackson (ObjectMapper) pour parser le JSON.
     * Chaque evaluation doit pouvoir resoudre 2 references :
     *   - son auteur (utilisateur) par son nom
     *   - son resume cible par (code_cours, titre)
     * Si l'une des deux references manque, l'evaluation est ignoree.
     */
    private void importerEvaluations() {
        Rapport r = new Rapport();
        try (InputStream in = new ClassPathResource("data/commentaires.json").getInputStream()) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode racine = mapper.readTree(in); // parse le JSON en arbre generique
            JsonNode evals = racine.get("evaluations");
            if (evals == null || !evals.isArray()) {
                // Structure JSON inattendue : on abandonne l'import des evaluations
                log.error("[INIT][eval] champ 'evaluations' absent ou invalide dans commentaires.json");
                return;
            }

            for (JsonNode e : evals) {
                try {
                    // Extraction des champs via texteJson() (gere les valeurs null)
                    String auteur = texteJson(e, "auteur");
                    JsonNode resNode = e.get("resume");
                    String coursCode = resNode != null ? texteJson(resNode, "cours") : null;
                    String titre = resNode != null ? texteJson(resNode, "titre") : null;
                    String noteStr = texteJson(e, "note");
                    String commentaire = texteJson(e, "commentaire");

                    // Premiere reference a resoudre : l'evaluateur (utilisateur) par son nom
                    Optional<Utilisateur> evaluateur = (auteur != null)
                            ? utilisateurRepository.findByNom(auteur.trim())
                            : Optional.empty();
                    if (evaluateur.isEmpty()) {
                        log.warn("[INIT][eval] auteur '{}' introuvable — evaluation ignoree", auteur);
                        r.skip();
                        continue;
                    }

                    // Deuxieme reference : le resume, identifie par (code cours + titre)
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
                            .dateEval(LocalDate.now()) // date non fournie dans la source
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

    /**
     * Cherche un resume par code de cours et titre (best effort).
     * Strategie : on liste tous les resumes du cours puis on filtre sur le titre
     * (comparaison insensible a la casse). Retourne null si rien trouve.
     */
    private Resume trouverResume(String coursCode, String titre) {
        if (coursCode == null || titre == null) return null;
        return resumeRepository.findByCoursCode(coursCode.trim()).stream()
                .filter(res -> titre.trim().equalsIgnoreCase(res.getTitre()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Ouvre un fichier du classpath (dossier resources/) en lecture UTF-8.
     * Le BufferedReader retourne sera ferme automatiquement par le try-with-resources de l'appelant.
     */
    private BufferedReader lire(String chemin) throws Exception {
        InputStream in = new ClassPathResource(chemin).getInputStream();
        return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    /**
     * Charge et parse un fichier XML du classpath en un Document DOM.
     * normalize() fusionne les noeuds texte adjacents pour eviter des surprises lors du parcours.
     */
    private Document parserXml(String chemin) throws Exception {
        try (InputStream in = new ClassPathResource(chemin).getInputStream()) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            Document doc = factory.newDocumentBuilder().parse(in);
            doc.getDocumentElement().normalize();
            return doc;
        }
    }

    /**
     * Renvoie le texte du premier sous-element <balise> direct ou descendant, ou null.
     *
     * Utilitaire interne utilise par les methodes d'import pour extraire facilement
     * la valeur textuelle d'un champ XML sans repeter getElementsByTagName + getTextContent partout.
     *
     * Comportement :
     *   - Aucun element <balise> trouve dans parent  -> retourne null
     *   - Plusieurs elements portent le meme nom     -> on prend uniquement le premier
     *   - Element existe mais vide                   -> retourne une chaine vide ""
     *
     * Le null permet aux appelants de detecter un champ absent et de decider
     * eux-memes (ignorer l'enregistrement, valeur par defaut...) sans exception.
     */
    private String texte(Element parent, String balise) {
        NodeList nl = parent.getElementsByTagName(balise);
        if (nl.getLength() == 0) return null;
        // On se limite au premier element rencontre.
        Node n = nl.item(0);
        return n != null ? n.getTextContent() : null;
    }

    /**
     * Renvoie le premier sous-element direct portant ce nom de balise, ou null.
     *
     * Difference avec texte() : on parcourt UNIQUEMENT les enfants directs
     * (pas les descendants en profondeur). Utile pour distinguer les
     * sections <resumes> et <achats> imbriquees dans <utilisateur> sans
     * remonter accidentellement des elements plus profonds.
     */
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

    /**
     * Extrait la valeur textuelle d'un champ JSON, ou null si absent/null.
     * Equivalent de texte() mais pour Jackson au lieu du DOM XML.
     */
    private String texteJson(JsonNode node, String champ) {
        JsonNode v = node.get(champ);
        return (v != null && !v.isNull()) ? v.asText() : null;
    }

    /**
     * Genere un hash simple a partir d'une chaine.
     * Meme logique que UtilisateurService, pour rester coherent : un utilisateur
     * importe ici doit pouvoir se connecter ensuite via le service normal.
     * NB : ce n'est PAS un hash cryptographique, juste un marqueur pour l'exercice.
     */
    private String simpleHash(String input) {
        return "$h$" + Integer.toHexString(input.hashCode());
    }
}