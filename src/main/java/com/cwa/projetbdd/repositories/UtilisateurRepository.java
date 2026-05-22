package com.cwa.projetbdd.repositories;

import com.cwa.projetbdd.models.Utilisateur;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Integer> {

    Optional<Utilisateur> findByNom(String nom);
    Optional<Utilisateur> findByEmail(String email);
    boolean existsByNom(String nom);
    boolean existsByEmail(String email);

    List<Utilisateur> findAllByOrderByPointsDesc();

    /** Q1 : Top N utilisateurs par points */
    @Query("SELECT u FROM Utilisateur u ORDER BY u.points DESC")
    List<Utilisateur> findTopByPoints(Pageable pageable);

    /** Q2 : Utilisateurs ayant publie dans au moins N cours differents */
    @Query("""
        SELECT u FROM Utilisateur u
        WHERE (
            SELECT COUNT(DISTINCT r.cours.code) FROM Resume r WHERE r.auteur.uid = u.uid
        ) >= :minCours
        """)
    List<Utilisateur> findUsersWithResumesInAtLeastNCourses(int minCours);

    /** Q5 : Utilisateurs n'ayant jamais publie de resume */
    @Query("""
        SELECT u FROM Utilisateur u
        WHERE u.uid NOT IN (SELECT DISTINCT r.auteur.uid FROM Resume r)
        """)
    List<Utilisateur> findUsersWithoutResume();

    /** Q7 : Utilisateurs ayant depense plus que leurs points disponibles — utilise total_depense */
    @Query("SELECT u FROM Utilisateur u WHERE u.totalDepense > u.points")
    List<Utilisateur> findOverspenders();
}
