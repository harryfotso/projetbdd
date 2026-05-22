package com.cwa.projetbdd.services;

import com.cwa.projetbdd.dto.DTOs.*;
import com.cwa.projetbdd.models.*;
import com.cwa.projetbdd.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Implementation des 8 requetes demandees par l'enonce + calcul du leaderboard.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final UtilisateurRepository utilisateurRepository;
    private final ResumeRepository resumeRepository;
    private final CoursRepository coursRepository;
    private final ObjetRepository objetRepository;
    private final SeuilNiveauRepository seuilNiveauRepository;
    private final PossessionRepository possessionRepository;

    /** Q1 : Top 10 utilisateurs par points */
    @Transactional(readOnly = true)
    public List<LeaderboardEntry> top10ParPoints() {
        List<Utilisateur> top = utilisateurRepository.findTopByPoints(PageRequest.of(0, 10));
        return mapToLeaderboard(top);
    }

    /** Leaderboard global complet */
    @Transactional(readOnly = true)
    public List<LeaderboardEntry> leaderboardComplet() {
        return mapToLeaderboard(utilisateurRepository.findAllByOrderByPointsDesc());
    }

    private List<LeaderboardEntry> mapToLeaderboard(List<Utilisateur> users) {
        int[] rang = {1};

        return users.stream().map(u -> {

            Integer niveau = seuilNiveauRepository.findNiveauByPoints(u.getPoints());

            Possession titreP = possessionRepository
                    .findActivePossessionByType(u.getUid(), TypeObjet.titre)
                    .orElse(null);

            Possession badgeP = possessionRepository
                    .findActivePossessionByType(u.getUid(), TypeObjet.badge)
                    .orElse(null);

            return LeaderboardEntry.builder()
                    .rang(rang[0]++)
                    .uid(u.getUid())
                    .nom(u.getNom())
                    .points(u.getPoints())
                    .niveau(niveau != null ? niveau : 1)
                    .titreActif(titreP != null ? titreP.getObjet().getNom() : null)
                    .badgeActif(badgeP != null ? badgeP.getObjet().getNom() : null)
                    .build();

        }).toList();
    }

    /** Q2 : Utilisateurs ayant publie dans au moins 3 cours differents */
    @Transactional(readOnly = true)
    public List<UtilisateurDTO> utilisateursMultiCours() {
        return utilisateurRepository.findUsersWithResumesInAtLeastNCourses(3)
                .stream()
                .map(UtilisateurDTO::from)
                .toList();
    }

    /** Q3 : Cours ayant le plus de resumes publies */
    @Transactional(readOnly = true)
    public CoursPlusResumes coursLePlusResume() {

        List<Object[]> rows = coursRepository.findCoursAvecPlusDeResumes();

        if (rows.isEmpty()) {
            return null;
        }

        Object[] r = rows.get(0);

        return CoursPlusResumes.builder()
                .code((String) r[0])
                .nom((String) r[1])
                .nbResumes(((Number) r[5]).longValue())
                .build();
    }

    /** Q4 : Resumes les mieux notes pour chaque cours */
    @Transactional(readOnly = true)
    public List<MeilleurResumeParCours> meilleursResumesParCours() {

        return resumeRepository.findMeilleurResumeParCours()
                .stream()
                .map(r -> MeilleurResumeParCours.builder()
                        .rid(((Number) r[0]).intValue())
                        .titre((String) r[1])
                        .codeCours((String) r[2])
                        .noteMoyenne(new BigDecimal(r[4].toString()))
                        .build())
                .toList();
    }

    /** Q5 : Utilisateurs n'ayant jamais publie de resume */
    @Transactional(readOnly = true)
    public List<UtilisateurDTO> utilisateursSansResume() {

        return utilisateurRepository.findUsersWithoutResume()
                .stream()
                .map(UtilisateurDTO::from)
                .toList();
    }

    /** Q6 : Objet cosmetique le plus achete */
    @Transactional(readOnly = true)
    public ObjetPlusAchete objetLePlusAchete() {

        List<Object[]> rows = objetRepository.findObjetLePlusAchete();

        if (rows.isEmpty()) {
            return null;
        }

        Object[] r = rows.get(0);

        return ObjetPlusAchete.builder()
                .oid(((Number) r[0]).intValue())
                .nom((String) r[1])
                .type(TypeObjet.valueOf((String) r[2]))
                .nbAchats(((Number) r[4]).longValue())
                .build();
    }

    /** Q7 : Utilisateurs ayant depense plus de points qu'ils n'en ont disponibles */
    @Transactional(readOnly = true)
    public List<Overspender> overspenders() {

        return utilisateurRepository.findOverspenders()
                .stream()
                .map(u -> Overspender.builder()
                        .uid(u.getUid())
                        .nom(u.getNom())
                        .pointsActuels(u.getPoints())
                        .totalDepense(u.getTotalDepense().longValue())
                        .build())
                .toList();
    }

    /** Q8 : Nombre moyen de resumes par utilisateur */
    @Transactional(readOnly = true)
    public Double moyenneResumesParUtilisateur() {

        Double r = resumeRepository.moyenneResumesParUtilisateur();

        return r != null ? r : 0.0;
    }
}