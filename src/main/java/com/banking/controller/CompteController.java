package com.banking.controller;

import com.banking.dto.BankingDtos.*;
import com.banking.entity.Utilisateur;
import com.banking.service.CompteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Contrôleur REST pour la gestion des comptes bancaires.
 * Tous les endpoints (sauf auth) nécessitent un token JWT valide.
 */
@RestController
@RequestMapping("/comptes")
@RequiredArgsConstructor
@Tag(name = "Comptes Bancaires", description = "Gestion des comptes et opérations financières")
@SecurityRequirement(name = "bearerAuth")
public class CompteController {

    private final CompteService compteService;

    // ==================== UC-01 : CRÉER UN COMPTE ====================

    @PostMapping
    @Operation(summary = "Créer un compte bancaire", description = "Crée un nouveau compte avec un solde initial de 0")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Compte créé avec succès"),
        @ApiResponse(responseCode = "400", description = "Données invalides"),
        @ApiResponse(responseCode = "409", description = "Email déjà utilisé")
    })
    public ResponseEntity<CompteResponse> creerCompte(
            @Valid @RequestBody CreerCompteRequest request,
            @AuthenticationPrincipal Utilisateur utilisateur) {

        CompteResponse response = compteService.creerCompte(request, utilisateur.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ==================== UC-02 : CONSULTER LES COMPTES ====================

    @GetMapping
    @Operation(summary = "Lister tous les comptes (Admin)", description = "Retourne la liste paginée de tous les comptes")
    @ApiResponse(responseCode = "200", description = "Liste des comptes retournée")
    public ResponseEntity<Page<CompteResponse>> listerTousLesComptes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int taille) {

        Page<CompteResponse> comptes = compteService.listerTousLesComptes(
                PageRequest.of(page, taille, Sort.by("dateCreation").descending())
        );
        return ResponseEntity.ok(comptes);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un compte", description = "Retourne les détails d'un compte par son ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Compte trouvé"),
        @ApiResponse(responseCode = "403", description = "Accès non autorisé"),
        @ApiResponse(responseCode = "404", description = "Compte introuvable")
    })
    public ResponseEntity<CompteResponse> getCompte(@PathVariable UUID id) {
        return ResponseEntity.ok(compteService.getCompteById(id));
    }

    // ==================== UC-03 : DÉPÔT ====================

    @PostMapping("/{id}/depot")
    @Operation(summary = "Effectuer un dépôt", description = "Crédite le montant spécifié sur le compte")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dépôt effectué avec succès"),
        @ApiResponse(responseCode = "400", description = "Montant invalide (doit être > 0)"),
        @ApiResponse(responseCode = "404", description = "Compte introuvable"),
        @ApiResponse(responseCode = "409", description = "Transaction dupliquée")
    })
    public ResponseEntity<OperationResponse> effectuerDepot(
            @PathVariable UUID id,
            @Valid @RequestBody OperationRequest request) {

        return ResponseEntity.ok(compteService.effectuerDepot(id, request));
    }

    // ==================== UC-04 : RETRAIT ====================

    @PostMapping("/{id}/retrait")
    @Operation(summary = "Effectuer un retrait", description = "Débite le montant spécifié du compte")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Retrait effectué avec succès"),
        @ApiResponse(responseCode = "400", description = "Montant invalide"),
        @ApiResponse(responseCode = "404", description = "Compte introuvable"),
        @ApiResponse(responseCode = "422", description = "Solde insuffisant")
    })
    public ResponseEntity<OperationResponse> effectuerRetrait(
            @PathVariable UUID id,
            @Valid @RequestBody OperationRequest request) {

        return ResponseEntity.ok(compteService.effectuerRetrait(id, request));
    }

    // ==================== HISTORIQUE ====================

    @GetMapping("/{id}/transactions")
    @Operation(summary = "Historique des transactions", description = "Retourne l'historique paginé des transactions")
    public ResponseEntity<Page<TransactionResponse>> getHistorique(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int taille) {

        Page<TransactionResponse> historique = compteService.getHistoriqueTransactions(
                id, PageRequest.of(page, taille)
        );
        return ResponseEntity.ok(historique);
    }
}
