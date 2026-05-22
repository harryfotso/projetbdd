package com.cwa.projetbdd.repositories;

import com.cwa.projetbdd.models.Cours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoursRepository extends JpaRepository<Cours, String> {

    List<Cours> findByFaculteIgnoreCase(String faculte);
    List<Cours> findByCredits(Integer credits);
    List<Cours> findByNomContainingIgnoreCaseOrCodeContainingIgnoreCase(String nom, String code);

    /** Q3 : Cours ayant le plus de resumes publies — utilise l'attribut dérivé nb_resumes */
    @Query(value = """
        SELECT c.code, c.nom, c.faculte, c.credits, c.annee_academique, c.nb_resumes
        FROM cours c
        ORDER BY c.nb_resumes DESC
        LIMIT 1
        """, nativeQuery = true)
    List<Object[]> findCoursAvecPlusDeResumes();
}
