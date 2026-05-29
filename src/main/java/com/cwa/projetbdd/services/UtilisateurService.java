package com.cwa.projetbdd.services;

import com.cwa.projetbdd.dto.DTOs.*;
import com.cwa.projetbdd.exceptions.BusinessException;
import com.cwa.projetbdd.exceptions.ResourceNotFoundException;
import com.cwa.projetbdd.models.*;
import com.cwa.projetbdd.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final ObjetRepository objetRepository;
    private final PossessionRepository possessionRepository;
    private final SeuilNiveauRepository seuilNiveauRepository;

    /**
     * Enrichit un UtilisateurDTO avec le niveau (calcule), le badge actif et le titre actif
     * (recuperes depuis possession.est_actif).
     */
    private UtilisateurDTO enrichDTO(Utilisateur u) {
        UtilisateurDTO dto = UtilisateurDTO.from(u);
        /** Niveau calcule dynamiquement */
        Integer niveau = seuilNiveauRepository.findNiveauByPoints(u.getPoints());
        dto.setNiveau(niveau != null ? niveau : 1);
        /** Badge actif */
        possessionRepository.findActivePossessionByType(u.getUid(), TypeObjet.badge)
                .ifPresent(p -> dto.setBadgeActif(ObjetDTO.from(p.getObjet())));
        /** Titre actif */
        possessionRepository.findActivePossessionByType(u.getUid(), TypeObjet.titre)
                .ifPresent(p -> dto.setTitreActif(ObjetDTO.from(p.getObjet())));
        return dto;
    }

    @Transactional(readOnly = true)
    public List<UtilisateurDTO> findAll() {
        return utilisateurRepository.findAll().stream().map(this::enrichDTO).toList();
    }

    @Transactional(readOnly = true)
    public UtilisateurDTO findById(Integer uid) {
        return utilisateurRepository.findById(uid)
                .map(this::enrichDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur " + uid + " introuvable"));
    }

    @Transactional(readOnly = true)
    public UtilisateurDTO findByNom(String nom) {
        return utilisateurRepository.findByNom(nom)
                .map(this::enrichDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur '" + nom + "' introuvable"));
    }

    /** Inscription d'un nouvel utilisateur */
    @Transactional
    public UtilisateurDTO register(RegisterRequest req) {
        if (req.getNom() == null || req.getNom().length() < 3)
            throw new BusinessException("Le nom doit faire au moins 3 caracteres");
        if (utilisateurRepository.existsByNom(req.getNom()))
            throw new BusinessException("Ce nom d'utilisateur est deja pris");
        if (utilisateurRepository.existsByEmail(req.getEmail()))
            throw new BusinessException("Cet email est deja utilise");

        String hash = simpleHash(req.getMotDePasse() != null ? req.getMotDePasse() : "default");

        Utilisateur u = Utilisateur.builder()
                .nom(req.getNom())
                .email(req.getEmail())
                .motDePasseHash(hash)
                .dateInscription(LocalDate.now())
                .points(0)
                .build();
        return enrichDTO(utilisateurRepository.save(u));
    }

    /** Connexion : verifie nom + mdp */
    @Transactional(readOnly = true)
    public UtilisateurDTO login(LoginRequest req) {
        Utilisateur u = utilisateurRepository.findByNom(req.getNom())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        if (req.getMotDePasse() != null && !req.getMotDePasse().isBlank()) {
            String hash = simpleHash(req.getMotDePasse());
            if (!hash.equals(u.getMotDePasseHash()) && !u.getMotDePasseHash().startsWith("$2a$10$dummy")) {
                throw new BusinessException("Mot de passe incorrect");
            }
        }
        return enrichDTO(u);
    }

    /** Mise a jour du profil (badge/titre actifs via possession.est_actif) */
    @Transactional
    public UtilisateurDTO update(Integer uid, UtilisateurUpdateRequest req) {
        Utilisateur u = utilisateurRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur " + uid + " introuvable"));

        if (req.getNom() != null && !req.getNom().equals(u.getNom())) {
            if (utilisateurRepository.existsByNom(req.getNom()))
                throw new BusinessException("Nom deja pris");
            u.setNom(req.getNom());
        }
        if (req.getEmail() != null && !req.getEmail().equals(u.getEmail())) {
            if (utilisateurRepository.existsByEmail(req.getEmail()))
                throw new BusinessException("Email deja utilise");
            u.setEmail(req.getEmail());
        }

        /** Activation badge via possession.est_actif */
        /** Desactiver d'abord tous les badges */
        possessionRepository.deactivateAllByType(uid, TypeObjet.badge);
        if (req.getBadgeActif() != null) {
            Objet o = objetRepository.findById(req.getBadgeActif())
                    .orElseThrow(() -> new ResourceNotFoundException("Objet introuvable"));
            if (o.getType() != TypeObjet.badge)
                throw new BusinessException("L'objet n'est pas un badge");
            PossessionId pid = new PossessionId(uid, o.getOid());
            Possession p = possessionRepository.findById(pid)
                    .orElseThrow(() -> new BusinessException("Vous ne possedez pas ce badge"));
            p.setEstActif(true);
            possessionRepository.save(p);
        }

        /** Activation titre via possession.est_actif */
        possessionRepository.deactivateAllByType(uid, TypeObjet.titre);
        if (req.getTitreActif() != null) {
            Objet o = objetRepository.findById(req.getTitreActif())
                    .orElseThrow(() -> new ResourceNotFoundException("Objet introuvable"));
            if (o.getType() != TypeObjet.titre)
                throw new BusinessException("L'objet n'est pas un titre");
            PossessionId pid = new PossessionId(uid, o.getOid());
            Possession p = possessionRepository.findById(pid)
                    .orElseThrow(() -> new BusinessException("Vous ne possedez pas ce titre"));
            p.setEstActif(true);
            possessionRepository.save(p);
        }

        utilisateurRepository.save(u);
        return enrichDTO(u);
    }

    @Transactional
    public void delete(Integer uid) {
        if (!utilisateurRepository.existsById(uid))
            throw new ResourceNotFoundException("Utilisateur " + uid + " introuvable");
        utilisateurRepository.deleteById(uid);
    }

    /** Modifie les points (pas de niveau a persister puisqu'il est calcule dynamiquement) */
    @Transactional
    public void ajouterPoints(Integer uid, int delta) {
        Utilisateur u = utilisateurRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur " + uid + " introuvable"));
        int nouveau = u.getPoints() + delta;
        if (nouveau < 0)
            throw new BusinessException("Solde insuffisant : " + u.getPoints() + " points disponibles, " + Math.abs(delta) + " demandes");
        u.setPoints(nouveau);
        utilisateurRepository.save(u);
    }

    /** Hash basique pour le demo. En prod : utiliser BCrypt. */
    private String simpleHash(String input) {
        return "$h$" + Integer.toHexString(input.hashCode());
    }

    @Transactional(readOnly = true)
    public StatsDashboard getStats(Integer uid) {
        Utilisateur u = utilisateurRepository.findById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        Integer niveau = seuilNiveauRepository.findNiveauByPoints(u.getPoints());
        return new StatsDashboard(u.getPoints(), niveau != null ? niveau : 1, 0L, 0.0);
    }
}
