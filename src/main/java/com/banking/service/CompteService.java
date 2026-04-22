package com.banking.service;

import com.banking.dto.BankingDtos.*;
import com.banking.entity.Compte;
import com.banking.entity.Transaction;
import com.banking.entity.Utilisateur;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompteService {

    private final CompteRepository compteRepository;
    private final TransactionRepository transactionRepository;
    private final UtilisateurRepository utilisateurRepository;

    // ========== UC-01 : CRÉER UN COMPTE ==========
    @Transactional
    public CompteResponse creerCompte(CreerCompteRequest request, UUID utilisateurId) {
        log.info("Création d'un compte pour l'utilisateur {}", utilisateurId);

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

        Compte saved = compteRepository.save(compte);
        log.info("Compte {} créé avec succès", saved.getNumeroCompte());
        return CompteResponse.fromEntity(saved);
    }

    // ========== UC-02 : CONSULTER UN COMPTE ==========
    @Transactional(readOnly = true)
    public CompteResponse getCompteById(UUID compteId) {
        Compte compte = compteRepository.findById(compteId)
                .orElseThrow(() -> new RuntimeException("Compte introuvable avec l'ID : " + compteId));
        return CompteResponse.fromEntity(compte);
    }

    @Transactional(readOnly = true)
    public Page<CompteResponse> listerTousLesComptes(Pageable pageable) {
        return compteRepository.findAll(pageable).map(CompteResponse::fromEntity);
    }

    // ========== UC-03 : DÉPÔT ==========
    @Transactional
    public OperationResponse effectuerDepot(UUID compteId, OperationRequest request) {
        log.info("Dépôt de {} sur le compte {}", request.getMontant(), compteId);

        verifierIdempotence(request.getReferenceExterne());

        Compte compte = getCompteActif(compteId);

        BigDecimal montant = request.getMontant().setScale(2, RoundingMode.HALF_UP);
        BigDecimal soldeAvant = compte.getSolde();
        BigDecimal soldeApres = soldeAvant.add(montant).setScale(2, RoundingMode.HALF_UP);

        compte.setSolde(soldeApres);
        compteRepository.save(compte);

        Transaction transaction = journaliserTransaction(
                compte, Transaction.TypeTransaction.DEPOT,
                montant, soldeAvant, soldeApres,
                request.getDescription(), request.getReferenceExterne()
        );

        log.info("Dépôt réussi — {} → solde: {} -> {}", compte.getNumeroCompte(), soldeAvant, soldeApres);

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

    // ========== UC-04 : RETRAIT ==========
    @Transactional
    public OperationResponse effectuerRetrait(UUID compteId, OperationRequest request) {
        log.info("Retrait de {} sur le compte {}", request.getMontant(), compteId);

        verifierIdempotence(request.getReferenceExterne());

        Compte compte = getCompteActif(compteId);

        BigDecimal montant = request.getMontant().setScale(2, RoundingMode.HALF_UP);
        BigDecimal soldeAvant = compte.getSolde();

        if (soldeAvant.compareTo(montant) < 0) {
            throw new RuntimeException(String.format(
                    "Solde insuffisant. Disponible : %.2f | Demandé : %.2f", soldeAvant, montant));
        }

        BigDecimal soldeApres = soldeAvant.subtract(montant).setScale(2, RoundingMode.HALF_UP);
        compte.setSolde(soldeApres);
        compteRepository.save(compte);

        Transaction transaction = journaliserTransaction(
                compte, Transaction.TypeTransaction.RETRAIT,
                montant, soldeAvant, soldeApres,
                request.getDescription(), request.getReferenceExterne()
        );

        log.info("Retrait réussi — {} → solde: {} -> {}", compte.getNumeroCompte(), soldeAvant, soldeApres);

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

    // ========== HISTORIQUE ==========
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getHistoriqueTransactions(UUID compteId, Pageable pageable) {
        compteRepository.findById(compteId)
                .orElseThrow(() -> new RuntimeException("Compte introuvable"));
        return transactionRepository
                .findByCompteIdOrderByDateTransactionDesc(compteId, pageable)
                .map(TransactionResponse::fromEntity);
    }

    // ========== MÉTHODES PRIVÉES ==========

    private Utilisateur getUtilisateurConnecte() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Utilisateur) authentication.getPrincipal();
    }

    private Compte getCompteActif(UUID compteId) {
        Compte compte = compteRepository.findById(compteId)
                .orElseThrow(() -> new RuntimeException("Compte introuvable avec l'ID : " + compteId));
        if (compte.getStatut() != Compte.StatutCompte.ACTIF) {
            throw new RuntimeException("Le compte est inactif ou suspendu : " + compte.getNumeroCompte());
        }
        return compte;
    }

    private void verifierIdempotence(String referenceExterne) {
        if (referenceExterne != null && !referenceExterne.isBlank()) {
            if (transactionRepository.existsByReferenceExterne(referenceExterne)) {
                throw new RuntimeException(
                        "Transaction dupliquée. Référence déjà traitée : " + referenceExterne);
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
                .referenceExterne(referenceExterne != null && !referenceExterne.isBlank()
                        ? referenceExterne : UUID.randomUUID().toString())
                .build();

        return transactionRepository.save(transaction);
    }

    private String genererNumeroCompte() {
        String numero;
        do {
            numero = "BK" + System.currentTimeMillis() % 1000000000L
                    + String.format("%04d", (int) (Math.random() * 10000));
        } while (compteRepository.findByNumeroCompte(numero).isPresent());
        return numero;
    }
}