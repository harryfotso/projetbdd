package com.cwa.projetbdd.repositories;

import com.cwa.projetbdd.models.UtilisateurClassement;
import com.cwa.projetbdd.models.UtilisateurClassementId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UtilisateurClassementRepository
        extends JpaRepository<UtilisateurClassement, UtilisateurClassementId> {

    List<UtilisateurClassement> findByClassementCidOrderByPlaceAsc(Integer cid);
}
