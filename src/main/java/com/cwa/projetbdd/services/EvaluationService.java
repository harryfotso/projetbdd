package com.cwa.projetbdd.services;

import com.cwa.projetbdd.dto.DTOs.*;
import com.cwa.projetbdd.exceptions.BusinessException;
import com.cwa.projetbdd.exceptions.ResourceNotFoundException;
import com.cwa.projetbdd.models.*;
import com.cwa.projetbdd.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EvaluationService {

    public static final int POINTS_EVALUATION_RECUE = 5;

    private final EvaluationRepository evaluationRepository;
    private final ResumeRepository resumeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final TransactionRepository transactionRepository;
    private final UtilisateurService utilisateurService;

    @Transactional(readOnly = true)
    public List<EvaluationDTO> findByResume(Integer rid) {
        return evaluationRepository.findByResumeRid(rid).stream().map(EvaluationDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public List<EvaluationDTO> findByEvaluateur(Integer uid) {
        return evaluationRepository.findByEvaluateurUid(uid).stream().map(EvaluationDTO::from).toList();
    }

    /** Creation d'une evaluation + transaction de gain pour l'auteur du resume */
    @Transactional
    public EvaluationDTO create(Integer evaluateurUid, EvaluationRequest req) {
        Utilisateur evaluateur = utilisateurRepository.findById(evaluateurUid)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluateur introuvable"));
        Resume r = resumeRepository.findById(req.getRid())
                .orElseThrow(() -> new ResourceNotFoundException("Resume introuvable"));

        if (r.getAuteur().getUid().equals(evaluateurUid))
            throw new BusinessException("Vous ne pouvez pas evaluer votre propre resume");

        if (evaluationRepository.existsByEvaluateurUidAndResumeRid(evaluateurUid, req.getRid()))
            throw new BusinessException("Vous avez deja evalue ce resume");

        if (req.getNote() == null
                || req.getNote().compareTo(BigDecimal.ZERO) < 0
                || req.getNote().compareTo(BigDecimal.valueOf(5)) > 0)
            throw new BusinessException("La note doit etre entre 0 et 5");

        if (LocalDate.now().isBefore(r.getDatePublication()))
            throw new BusinessException("Date d'evaluation invalide");

        Evaluation e = Evaluation.builder()
                .note(req.getNote())
                .commentaire(req.getCommentaire())
                .dateEval(LocalDate.now())
                .evaluateur(evaluateur)
                .resume(r)
                .build();
        e = evaluationRepository.save(e);

        // Transaction : l'auteur du resume gagne 5 points
        Transaction t = Transaction.builder()
                .typeTransaction(TypeTransaction.GAIN_EVALUATION)
                .dateTransaction(LocalDate.now())
                .montant(POINTS_EVALUATION_RECUE)
                .utilisateur(r.getAuteur())
                .evaluation(e)
                .build();
        transactionRepository.save(t);
        utilisateurService.ajouterPoints(r.getAuteur().getUid(), POINTS_EVALUATION_RECUE);

        return EvaluationDTO.from(e);
    }

    @Transactional
    public void delete(Integer eid, Integer uid) {
        Evaluation e = evaluationRepository.findById(eid)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation introuvable"));
        if (!e.getEvaluateur().getUid().equals(uid))
            throw new BusinessException("Seul l'auteur de l'evaluation peut la supprimer");
        evaluationRepository.delete(e);
    }
}
