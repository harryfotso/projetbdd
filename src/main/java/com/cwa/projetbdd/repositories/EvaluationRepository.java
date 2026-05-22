package com.cwa.projetbdd.repositories;

import com.cwa.projetbdd.models.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Integer> {

    List<Evaluation> findByResumeRid(Integer rid);
    List<Evaluation> findByEvaluateurUid(Integer uid);
    Optional<Evaluation> findByEvaluateurUidAndResumeRid(Integer uid, Integer rid);
    boolean existsByEvaluateurUidAndResumeRid(Integer uid, Integer rid);
}
