package com.cwa.projetbdd.repositories;

import com.cwa.projetbdd.models.Objet;
import com.cwa.projetbdd.models.TypeObjet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ObjetRepository extends JpaRepository<Objet, Integer> {

    List<Objet> findByType(TypeObjet type);

    /** Recherche d'un objet par son nom (utilise par l'import des achats). */
    Optional<Objet> findByNom(String nom);

    /** Q6 : L'objet cosmetique le plus achete — utilise l'attribut dérivé nb_achats */
    @Query(value = """
        SELECT o.oid, o.nom, o.type, o.prix, o.nb_achats
        FROM objet o
        ORDER BY o.nb_achats DESC
        LIMIT 1
        """, nativeQuery = true)
    List<Object[]> findObjetLePlusAchete();
}
