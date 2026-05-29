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
public class PossessionService {

    private final PossessionRepository possessionRepository;
    private final ObjetRepository objetRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final TransactionRepository transactionRepository;
    private final UtilisateurService utilisateurService;

    @Transactional(readOnly = true)
    public List<PossessionDTO> findByUtilisateur(Integer uid) {
        return possessionRepository.findByUtilisateurUid(uid).stream()
                .map(PossessionDTO::from).toList();
    }

    /** Achat d'un objet : verifie le solde, cree Possession + Transaction, debite */
    @Transactional
    public PossessionDTO acheter(AchatRequest req) {
        Utilisateur u = utilisateurRepository.findById(req.getUid())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        Objet o = objetRepository.findById(req.getOid())
                .orElseThrow(() -> new ResourceNotFoundException("Objet introuvable"));

        if (possessionRepository.existsByUtilisateurUidAndObjetOid(u.getUid(), o.getOid()))
            throw new BusinessException("Vous possedez deja cet objet");
        if (u.getPoints() < o.getPrix())
            throw new BusinessException("Solde insuffisant : " + u.getPoints() + " pts < " + o.getPrix() + " pts");

        Possession p = Possession.builder()
                .utilisateur(u).objet(o).dateAchat(LocalDate.now()).estActif(false).build();
        possessionRepository.save(p);

        if (o.getPrix() > 0) {
            Transaction t = Transaction.builder()
                    .typeTransaction(TypeTransaction.DEPENSE)
                    .dateTransaction(LocalDate.now())
                    .montant(-o.getPrix())
                    .utilisateur(u)
                    .objet(o)
                    .build();
            transactionRepository.save(t);
            utilisateurService.ajouterPoints(u.getUid(), -o.getPrix());
        }

        return PossessionDTO.from(p);
    }

    /**
     * Active un objet (badge ou titre) pour un utilisateur.
     * Desactive d'abord tout autre objet du meme type.
     */
    @Transactional
    public void activerObjet(Integer uid, Integer oid) {
        PossessionId pid = new PossessionId(uid, oid);
        Possession p = possessionRepository.findById(pid)
                .orElseThrow(() -> new ResourceNotFoundException("Possession introuvable"));

        TypeObjet type = p.getObjet().getType();
        if (type != TypeObjet.badge && type != TypeObjet.titre)
            throw new BusinessException("Seuls les badges et titres peuvent etre actives");

        /** Désactive tous les objets du même type pour cet utilisateur */
        possessionRepository.deactivateAllByType(uid, type);
        /** Active l'objet sélectionné. */
        p.setEstActif(true);
        possessionRepository.save(p);
    }
}
