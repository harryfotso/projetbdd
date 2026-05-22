package com.cwa.projetbdd.repositories;

import com.cwa.projetbdd.models.SeuilNiveau;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SeuilNiveauRepository extends JpaRepository<SeuilNiveau, Integer> {

    /**
     * Calcule le niveau d'un utilisateur a partir de ses points.
     * Retourne le plus grand niveau dont le seuil est <= aux points donnes.
     */
    @Query("SELECT MAX(s.niveau) FROM SeuilNiveau s WHERE s.pointsMin <= :points")
    Integer findNiveauByPoints(@Param("points") Integer points);
}
