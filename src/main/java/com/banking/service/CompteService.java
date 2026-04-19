package com.banking.service;

import com.banking.dto.BankingDtos.*;
import com.banking.entity.Compte;
import com.banking.entity.Transaction;
import com.banking.entity.Utilisateur;
import com.banking.exception.GlobalExceptionHandler;
import com.banking.repository.CompteRepository;
import com.banking.repository.TransactionRepository;
import com.banking.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service métier pour la gestion des comptes bancaires.
 *
 * Principes appliqués :
 * - @Transactional : garantit l'atomicité des opérations (ACID)
 * - BigDecimal avec scale=2 : précision des calculs financiers
 * - Idempotence : vérification des références externes dupliquées
 * - Journalisation systématique de chaque opération
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompteService {

    private final CompteRepository compteRepository;
    private final TransactionRepository transactionRepository;
    private final UtilisateurRepository utilisateurRepository;

    /**
     * UC-01 : Crée un nouveau compte bancaire.
     * @throws RuntimeException si l'email est déjà utilisé
     */
    @Transactional
    public CompteResponse creerCompte(CreerCompteRequest request, UUID utilisateurId) {
        log.info("Création d'un compte pour l'utilisateur {}", utilisateurId);

        // Vérifier unicité de l'email
        if (compteRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Un compte avec cet email existe déjà : " + request.getEmail());
        }

        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Compte compte = Compte.builder()
                .numeroCompte(genererNumeroCompte())
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(request.getEmail())
                .solde(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .statut(Compte.StatutCompte.ACTIF)
                .utilisateur(utilisateur)
                .build();

        Compte compteEnregistre = compteRepository.save(compte);
        log.info("Compte {} créé avec succès", compteEnregistre.getNumeroCompte());

        return CompteResponse.fromEntity(compteEnregistre);
    }

    /**
     * UC-02 : Récupère un compte par son ID.
     * Vérifie que le client n'accède qu'à son propre compte (sauf admin).
     */
    @Transactional(readOnly = true)
    public CompteResponse getCompteById(UUID compteId) {
        Compte compte = compteRepository.findById(compteId)
                .orElseThrow(() -> new RuntimeException("Compte introuvable avec l'ID : " + compteId));

        verifierAccesCompte(compte);

        return CompteResponse.fromEntity(compte);
    }

    /**
     * UC-02 : Liste tous les comptes (admin uniquement, paginé).
     */
    @Transactional(readOnly = true)
    public Page<CompteResponse> listerTousLesComptes(Pageable pageable) {
        return compteRepository.findAll(pageable).map(CompteResponse::fromEntity);
    }

    /**
     * UC-03 : Effectue un dépôt sur un compte.
     *
     * Garanties :
     * - Opération atomique (@Transactional)
     * - Précision BigDecimal sur 2 décimales
     * - Idempotence via referenceExterne
     * - Journalisation de la transaction
     */
    @Transactional
    public OperationResponse effectuerDepot(UUID compteId, OperationRequest request) {
        log.info("Dépôt de {} sur le compte {}", request.getMontant(), compteId);

        // Vérifier l'idempotence
        verifierIdempotence(request.getReferenceExterne());

        Compte compte = getCompteActif(compteId);
        verifierAccesCompte(compte);

        BigDecimal montant = request.getMontant().setScale(2, RoundingMode.HALF_UP);
        BigDecimal soldeAvant = compte.getSolde();

        // Calcul du nouveau solde (BigDecimal — pas de perte de précision)
        BigDecimal soldeApres = soldeAvant.add(montant).setScale(2, RoundingMode.HALF_UP);
        compte.setSolde(soldeApres);
        compteRepository.save(compte);

        // Journalisation de la transaction
        Transaction transaction = journaliserTransaction(
                compte, Transaction.TypeTransaction.DEPOT,
                montant, soldeAvant, soldeApres,
                request.getDescription(), request.getReferenceExterne()
        );

        log.info("Dépôt réussi — compte {} : {} -> {}", compte.getNumeroCompte(), soldeAvant, soldeApres);

        return OperationResponse.builder()
                .message("Dépôt effectué avec succès")
                .montant(montant)
                .soldeAvant(soldeAvant)
                .soldeApres(soldeApres)
                .typeTransaction(Transaction.TypeTransaction.DEPOT)
                .transactionId(transaction.getId())
                .dateOperation(transaction.getDateTransaction())
                .build();
    }

    /**
     * UC-04 : Effectue un retrait sur un compte.
     *
     * Garanties identiques au dépôt + vérification du solde suffisant.
     * En cas d'erreur, @Transactional assure le rollback automatique.
     */
    @Transactional
    public OperationResponse effectuerRetrait(UUID compteId, OperationRequest request) {
        log.info("Retrait de {} sur le compte {}", request.getMontant(), compteId);

        verifierIdempotence(request.getReferenceExterne());

        Compte compte = getCompteActif(compteId);
        verifierAccesCompte(compte);

        BigDecimal montant = request.getMontant().setScale(2, RoundingMode.HALF_UP);
        BigDecimal soldeAvant = compte.getSolde();

        // Vérification du solde suffisant (règle métier critique)
        if (soldeAvant.compareTo(montant) < 0) {
            throw new RuntimeException(String.format(
                    "Solde insuffisant. Solde disponible : %.2f | Montant demandé : %.2f",
                    soldeAvant, montant
            ));
        }

        BigDecimal soldeApres = soldeAvant.subtract(montant).setScale(2, RoundingMode.HALF_UP);
        compte.setSolde(soldeApres);
        compteRepository.save(compte);

        Transaction transaction = journaliserTransaction(
                compte, Transaction.TypeTransaction.RETRAIT,
                montant, soldeAvant, soldeApres,
                request.getDescription(), request.getReferenceExterne()
        );

        log.info("Retrait réussi — compte {} : {} -> {}", compte.getNumeroCompte(), soldeAvant, soldeApres);

        return OperationResponse.builder()
                .message("Retrait effectué avec succès")
                .montant(montant)
                .soldeAvant(soldeAvant)
                .soldeApres(soldeApres)
                .typeTransaction(Transaction.TypeTransaction.RETRAIT)
                .transactionId(transaction.getId())
                .dateOperation(transaction.getDateTransaction())
                .build();
    }

    /**
     * Récupère l'historique paginé des transactions d'un compte.
     */
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getHistoriqueTransactions(UUID compteId, Pageable pageable) {
        Compte compte = compteRepository.findById(compteId)
                .orElseThrow(() -> new RuntimeException("Compte introuvable"));
        verifierAccesCompte(compte);

        return transactionRepository
                .findByCompteIdOrderByDateTransactionDesc(compteId, pageable)
                .map(TransactionResponse::fromEntity);
    }

    // ==================== MÉTHODES PRIVÉES ====================

    private Compte getCompteActif(UUID compteId) {
        Compte compte = compteRepository.findById(compteId)
                .orElseThrow(() -> new RuntimeException("Compte introuvable avec l'ID : " + compteId));

        if (compte.getStatut() != Compte.StatutCompte.ACTIF) {
            throw new RuntimeException("Le compte est inactif ou suspendu : " + compte.getNumeroCompte());
        }
        return compte;
    }

    private void verifierAccesCompte(Compte compte) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var utilisateur = (com.banking.entity.Utilisateur) authentication.getPrincipal();

        // L'admin a accès à tout
        if (utilisateur.getRole() == Utilisateur.Role.ADMIN) return;

        // Le client ne peut accéder qu'à ses propres comptes
        if (!compte.getUtilisateur().getId().equals(utilisateur.getId())) {
            throw new RuntimeException("Accès non autorisé à ce compte");
        }
    }

    private void verifierIdempotence(String referenceExterne) {
        if (referenceExterne != null && !referenceExterne.isBlank()) {
            if (transactionRepository.existsByReferenceExterne(referenceExterne)) {
                throw new RuntimeException(
                        "Transaction dupliquée détectée. Référence déjà traitée : " + referenceExterne);
            }
        }
    }

    private Transaction journaliserTransaction(
            Compte compte, Transaction.TypeTransaction type,
            BigDecimal montant, BigDecimal soldeAvant, BigDecimal soldeApres,
            String description, String referenceExterne) {

        Transaction transaction = Transaction.builder()
                .compte(compte)
                .typeTransaction(type)
                .montant(montant)
                .soldeAvant(soldeAvant)
                .soldeApres(soldeApres)
                .description(description)
                .referenceExterne(referenceExterne != null ? referenceExterne : UUID.randomUUID().toString())
                .build();

        return transactionRepository.save(transaction);
    }

    /**
     * Génère un numéro de compte unique au format : BK + timestamp + aléatoire.
     */
    private String genererNumeroCompte() {
        String numero;
        do {
            numero = "BK" + System.currentTimeMillis() % 1000000000L
                    + String.format("%04d", (int)(Math.random() * 10000));
        } while (compteRepository.findByNumeroCompte(numero).isPresent());
        return numero;
    }
}
