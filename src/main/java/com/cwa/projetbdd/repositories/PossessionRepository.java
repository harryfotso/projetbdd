package com.cwa.projetbdd.repositories;

import com.cwa.projetbdd.models.Possession;
import com.cwa.projetbdd.models.PossessionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PossessionRepository extends JpaRepository<Possession, PossessionId> {

    List<Possession> findByUtilisateurUid(Integer uid);
    boolean existsByUtilisateurUidAndObjetOid(Integer uid, Integer oid);

    /**
     * Trouve la possession active (badge ou titre) pour un utilisateur et un type d'objet donne.
     */
    @Query("""
        SELECT p FROM Possession p
        JOIN p.objet o
        WHERE p.utilisateur.uid = :uid
          AND o.type = :type
          AND p.estActif = true
        """)
    Optional<Possession> findActivePossessionByType(@Param("uid") Integer uid,
                                                     @Param("type") com.cwa.projetbdd.models.TypeObjet type);

    /**
     * Desactive toutes les possessions d'un type donne pour un utilisateur.
     */
    @Modifying
    @Query("""
        UPDATE Possession p SET p.estActif = false
        WHERE p.utilisateur.uid = :uid
          AND p.objet.type = :type
        """)
    void deactivateAllByType(@Param("uid") Integer uid,
                              @Param("type") com.cwa.projetbdd.models.TypeObjet type);
}
