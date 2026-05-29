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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResumeService {

    public static final int POINTS_PUBLICATION = 10;

    private final ResumeRepository resumeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final CoursRepository coursRepository;
    private final TransactionRepository transactionRepository;
    private final UtilisateurService utilisateurService;

    @Transactional(readOnly = true)
    public List<ResumeDTO> findAll() {
        return resumeRepository.findAll().stream().map(this::enrich).toList();
    }

    @Transactional(readOnly = true)
    public List<ResumeDTO> findByCours(String code) {
        return resumeRepository.findByCoursCode(code).stream().map(this::enrich).toList();
    }

    @Transactional(readOnly = true)
    public List<ResumeDTO> findByAuteur(Integer uid) {
        return resumeRepository.findByAuteurUid(uid).stream().map(this::enrich).toList();
    }

    @Transactional(readOnly = true)
    public ResumeDTO findById(Integer rid) {
        Resume r = resumeRepository.findById(rid)
                .orElseThrow(() -> new ResourceNotFoundException("Resume " + rid + " introuvable"));
        return enrich(r);
    }

    /** Publication d'un resume + transaction de gain automatique */
    @Transactional
    public ResumeDTO publish(Integer auteurUid, ResumeRequest req) {
        Utilisateur auteur = utilisateurRepository.findById(auteurUid)
                .orElseThrow(() -> new ResourceNotFoundException("Auteur introuvable"));
        Cours cours = coursRepository.findById(req.getCodeCours())
                .orElseThrow(() -> new ResourceNotFoundException("Cours " + req.getCodeCours() + " introuvable"));

        if (req.getTitre() == null || req.getTitre().isBlank())
            throw new BusinessException("Titre obligatoire");
        if (req.getDescription() == null || req.getDescription().isBlank())
            throw new BusinessException("Description obligatoire");

        String vis = req.getVisibilite() != null ? req.getVisibilite() : "public";
        if (!vis.equals("public") && !vis.equals("prive"))
            throw new BusinessException("Visibilite invalide");

        Resume r = Resume.builder()
                .titre(req.getTitre())
                .description(req.getDescription())
                .datePublication(LocalDate.now())
                .version(1)
                .visibilite(vis)
                .auteur(auteur)
                .cours(cours)
                .build();
        r = resumeRepository.save(r);

        /** Transaction de gain */
        Transaction t = Transaction.builder()
                .typeTransaction(TypeTransaction.GAIN_PUBLICATION)
                .dateTransaction(LocalDate.now())
                .montant(POINTS_PUBLICATION)
                .utilisateur(auteur)
                .resume(r)
                .build();
        transactionRepository.save(t);
        utilisateurService.ajouterPoints(auteurUid, POINTS_PUBLICATION);

        return enrich(r);
    }

    /** Modification d'un resume : version++ */
    @Transactional
    public ResumeDTO update(Integer rid, Integer uid, ResumeRequest req) {
        Resume r = resumeRepository.findById(rid)
                .orElseThrow(() -> new ResourceNotFoundException("Resume " + rid + " introuvable"));
        if (!r.getAuteur().getUid().equals(uid))
            throw new BusinessException("Seul l'auteur peut modifier ce resume");

        if (req.getTitre() != null) r.setTitre(req.getTitre());
        if (req.getDescription() != null) r.setDescription(req.getDescription());
        if (req.getVisibilite() != null) {
            if (!req.getVisibilite().equals("public") && !req.getVisibilite().equals("prive"))
                throw new BusinessException("Visibilite invalide");
            r.setVisibilite(req.getVisibilite());
        }
        if (req.getCodeCours() != null && !req.getCodeCours().equals(r.getCours().getCode())) {
            Cours c = coursRepository.findById(req.getCodeCours())
                    .orElseThrow(() -> new ResourceNotFoundException("Cours introuvable"));
            r.setCours(c);
        }
        r.setVersion(r.getVersion() + 1);
        return enrich(resumeRepository.save(r));
    }

    @Transactional
    public void delete(Integer rid, Integer uid) {
        Resume r = resumeRepository.findById(rid)
                .orElseThrow(() -> new ResourceNotFoundException("Resume " + rid + " introuvable"));
        if (!r.getAuteur().getUid().equals(uid))
            throw new BusinessException("Seul l'auteur peut supprimer ce resume");
        resumeRepository.delete(r);
    }

    /** Enrichit le DTO avec note moyenne et nombre d'evaluations */
    public ResumeDTO enrich(Resume r) {
        ResumeDTO dto = ResumeDTO.from(r);
        Object[] mean = resumeRepository.noteMoyenne(r.getRid());
        if (mean != null && mean.length >= 3) {
            // mean array can be wrapped in Object[1]
            Object[] row = mean.length == 1 && mean[0] instanceof Object[] ? (Object[]) mean[0] : mean;
            if (row[1] != null) {
                BigDecimal moy = new BigDecimal(row[1].toString()).setScale(2, RoundingMode.HALF_UP);
                dto.setNoteMoyenne(moy);
            }
            if (row[2] != null) {
                dto.setNbEvaluations(((Number) row[2]).intValue());
            }
        }
        return dto;
    }
}
