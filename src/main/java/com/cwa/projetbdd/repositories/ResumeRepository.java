package com.cwa.projetbdd.repositories;

import com.cwa.projetbdd.models.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Integer> {

    List<Resume> findByAuteurUid(Integer uid);
    List<Resume> findByCoursCode(String code);
    List<Resume> findByCoursCodeAndVisibilite(String code, String visibilite);

    /**
     * Note moyenne d'un resume (ou null si aucune evaluation).
     * Renvoie [rid, AVG(note), COUNT(eid)].
     */
    @Query(value = """
        SELECT r.rid, AVG(e.note), COUNT(e.eid)
        FROM resume r
        LEFT JOIN evaluation e ON e.rid = r.rid
        WHERE r.rid = :rid
        GROUP BY r.rid
        """, nativeQuery = true)
    Object[] noteMoyenne(Integer rid);

    /** Q4 : Resumes les mieux notes pour chaque cours — utilise l'attribut dérivé note_moyenne */
    @Query(value = """
        SELECT r.rid, r.titre, r.code_cours, r.uid, r.note_moyenne
        FROM resume r
        WHERE r.note_moyenne IS NOT NULL
          AND r.note_moyenne = (
              SELECT MAX(r2.note_moyenne)
              FROM resume r2
              WHERE r2.code_cours = r.code_cours
          )
        ORDER BY r.code_cours
        """, nativeQuery = true)
    List<Object[]> findMeilleurResumeParCours();

    /** Q8 : Nombre moyen de resumes par utilisateur */
    @Query(value = """
        SELECT ROUND(COUNT(r.rid) * 1.0 / (SELECT COUNT(*) FROM utilisateur), 2)
        FROM resume r
        """, nativeQuery = true)
    Double moyenneResumesParUtilisateur();
}
