# INFOH303 — Plateforme de Partage de Résumés de Cours

> Projet de Base de Données — Partie 2 · Année 2025-2026 · Groupe E
> El Yassini Luqman · Hayat Zakaria · Fotso Souopgui Harry · Minacapelli Enzo

Application web full-stack permettant aux étudiants de publier des résumés de cours, de les évaluer, de gagner des points et de débloquer des objets cosmétiques.

---

## Stack technique

| Couche | Technologie |
|--------|-------------|
| Backend | Java 21, Spring Boot 3.3, Spring Data JPA |
| Base de données | MySQL 8+ (InnoDB, utf8mb4) |
| Frontend | HTML5, CSS3, JavaScript vanilla (SPA, hash routing) |
| Build | Maven |

---

## Prérequis

- **Java 21+** (vérifier avec `java -version`)
- **Maven 3.8+** (ou utiliser le wrapper `./mvnw` fourni)
- **MySQL 8+** en local (port 3306)

---

## Installation

### 1. Préparer MySQL

```sql
-- Connecte-toi a MySQL en root et execute :
CREATE DATABASE IF NOT EXISTS projetbdd
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Si ton mot de passe MySQL n'est pas `root`, modifie `src/main/resources/application.properties` :

```properties
spring.datasource.username=ton_user
spring.datasource.password=ton_mot_de_passe
```

### 2. Cloner le repo et lancer

```bash
git clone https://github.com/harryfotso/projetbdd
cd projetbdd

# Linux / macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

Au premier lancement, Spring Boot va :
1. Exécuter `schema.sql` (crée les 9 tables, FK, contraintes, index)
2. Exécuter `data.sql` (insère les 47 cours, 45 objets, 50 utilisateurs, 89 résumés, 92 évaluations, 147 possessions, 328 transactions)

Une fois démarré, ouvre **http://localhost:8080** dans ton navigateur.

### 3. Connexion

Tu peux te connecter avec n'importe quel utilisateur des données fournies (sans mot de passe pour les comptes existants). Exemples :

| Nom | Points | Niveau |
|-----|--------|--------|
| `alice_dupont` | 1250 | 5 |
| `romain_phillips` | 3200 | 9 |
| `bob_martin` | 2900 | 9 |

Ou crée un nouveau compte via le bouton "S'inscrire".

---

## Architecture du projet

```
projetbdd/
├── pom.xml                              # Dependances Maven
├── src/main/
│   ├── java/com/cwa/projetbdd/
│   │   ├── ProjetbddApplication.java    # Point d'entree
│   │   ├── models/                      # 9 entites JPA + enums + composite keys
│   │   ├── repositories/                # 9 Spring Data JPA repos
│   │   ├── services/                    # Logique metier (transactions, points, etc.)
│   │   ├── controllers/                 # 9 controllers REST
│   │   ├── dto/                         # DTOs pour I/O JSON
│   │   ├── exceptions/                  # Custom exceptions + handler global
│   │   └── config/                      # Config CORS
│   └── resources/
│       ├── application.properties       # Config DB et JPA
│       ├── schema.sql                   # DDL : tables + contraintes + index
│       ├── data.sql                     # Donnees d'initialisation
│       └── static/                      # Frontend SPA
│           ├── index.html               # Point d'entree
│           ├── css/style.css            # Style global
│           └── js/
│               ├── common.js            # Client API, Session, helpers
│               └── app.js               # Router + 12 ecrans
```

---

## Endpoints API

Base URL : `http://localhost:8080/api`

### Utilisateurs
- `GET    /utilisateurs` — Liste tous
- `GET    /utilisateurs/{uid}` — Détail
- `GET    /utilisateurs/by-nom/{nom}` — Par nom
- `POST   /utilisateurs/register` — Inscription `{nom, email, motDePasse}`
- `POST   /utilisateurs/login` — Connexion `{nom, motDePasse}`
- `PUT    /utilisateurs/{uid}` — Modifier (badge/titre actif notamment)
- `DELETE /utilisateurs/{uid}` — Supprimer

### Cours
- `GET    /cours?faculte=X&credits=N&q=search` — Liste filtrée
- `GET    /cours/{code}` — Détail
- `POST   /cours` — Ajouter

### Résumés
- `GET    /resumes?cours=CODE&auteur=UID` — Filtres
- `GET    /resumes/{rid}` — Détail (avec note moyenne)
- `POST   /resumes` — Publier (header `X-User-Id`) → +10 pts
- `PUT    /resumes/{rid}` — Modifier (header `X-User-Id`)
- `DELETE /resumes/{rid}` — Supprimer (header `X-User-Id`)

### Évaluations
- `GET    /evaluations?rid=RID` — Évaluations d'un résumé
- `GET    /evaluations?evaluateur=UID` — Évaluations d'un user
- `POST   /evaluations` — Évaluer (header `X-User-Id`) → +5 pts à l'auteur
- `DELETE /evaluations/{eid}`

### Objets / Boutique
- `GET    /objets?type=badge|titre|theme|cosmetique` — Catalogue
- `GET    /objets/{oid}` — Détail
- `GET    /possessions?uid=UID` — Inventaire d'un user
- `POST   /possessions` — Acheter `{uid, oid}` → -prix pts

### Transactions
- `GET    /transactions?uid=UID` — Historique chronologique

### Leaderboard
- `GET    /leaderboard?top=N` — Top N (ou tous si pas de paramètre)

### Analytics (les 8 requêtes imposées)
- `GET    /analytics/top10` — Q1
- `GET    /analytics/multi-cours` — Q2
- `GET    /analytics/cours-populaire` — Q3
- `GET    /analytics/best-resumes` — Q4
- `GET    /analytics/no-resume` — Q5
- `GET    /analytics/objet-populaire` — Q6
- `GET    /analytics/overspenders` — Q7
- `GET    /analytics/avg-resumes` — Q8

---

## Écrans (12)

| ID | Route | Description |
|----|-------|-------------|
| S0 | `#/login` | Connexion / Inscription |
| S1 | `#/dashboard` | Vue d'accueil avec stats |
| S2 | `#/cours` | Liste filtrable des cours |
| S3 | `#/cours/{code}` | Détail d'un cours + ses résumés |
| S4 | `#/resume-form` ou `#/resume-form/{rid}` | Publier / Modifier |
| S5 | `#/resume/{rid}` | Détail + évaluations |
| S6 | `#/profil/{uid}` | Profil utilisateur (3 onglets) |
| S7 | `#/leaderboard` | Classement avec podium |
| S8 | `#/boutique` | Catalogue + achat |
| S9 | `#/inventaire` | Mes objets + activation |
| S10 | `#/transactions` | Historique des points |
| S11 | `#/analytics` | Les 8 requêtes |

---

## Règles métier

- **Publication d'un résumé** → +10 points (transaction `publication`)
- **Évaluation reçue sur un de mes résumés** → +5 points pour moi (transaction `evaluation`)
- **Achat d'un objet** → -prix points (transaction `achat`)
- **Niveau** recalculé automatiquement après chaque modification de points (seuils dérivés des données fournies)
- **Anti-auto-évaluation** : un utilisateur ne peut pas évaluer son propre résumé (vérifié en service)
- **Une seule évaluation par résumé** : `UNIQUE(uid, rid)` en base
- **Solde non-négatif** : impossible d'acheter sans les fonds suffisants
- **Badge/Titre actif** : doit être un objet de bon type ET possédé par l'utilisateur

---

## Conformité avec l'énoncé

| Exigence | Statut |
|----------|--------|
| Inscription / Connexion / Profil | ✓ S0 + S6 |
| Consultation / Ajout de cours | ✓ S2 |
| Publication / Consultation / Modif / Suppression de résumés | ✓ S3, S4, S5 |
| Évaluation (note + commentaire) | ✓ S5 |
| Attribution automatique de points | ✓ Services |
| Solde + historique transactions | ✓ S10 |
| Leaderboard | ✓ S7 |
| Boutique + achat + activation | ✓ S8, S9 |
| Cohérence via SGBD (CHECK, FK, UNIQUE) | ✓ schema.sql |
| Script DDL | ✓ schema.sql |
| Script d'initialisation | ✓ data.sql (généré depuis les fichiers fournis) |
| Les 8 requêtes imposées | ✓ S11 + AnalyticsController |

---

## Remarques pour la défense

- Le **niveau** n'est pas dérivé d'une formule simple `floor(points/100)+1` car les seuils dans les données fournies ne suivent pas cette règle. Les seuils réels sont stockés dans `UtilisateurService.niveauPourPoints()`.
- L'**année académique** est conservée comme attribut (pas dans la PK) pour simplifier les jointures et le CRUD ; on a justifié ce choix dans la documentation E-A.
- La **note moyenne** des résumés est calculée à la volée par `ResumeRepository.noteMoyenne()` — pas stockée — pour éviter la complexité de triggers de mise à jour.
- Le mot de passe est haché en service simple (`simpleHash`) — en production on remplacerait par BCrypt.
- Pour les comptes pré-existants (issus des données fournies), la connexion accepte un mot de passe vide.

---

## Reset de la base

Pour repartir de zéro :

```sql
DROP DATABASE projetbdd;
CREATE DATABASE projetbdd CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Puis relance l'application — `schema.sql` et `data.sql` seront ré-exécutés.

---

## Licence

Projet académique — INFOH303, Université Libre de Bruxelles.
