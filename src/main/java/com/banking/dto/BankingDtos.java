package com.banking.dto;

import com.banking.entity.Compte;
import com.banking.entity.Transaction;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Classes DTO (Data Transfer Object) — séparation entre couche API et couche persistance.
 */
public class BankingDtos {

    // ==================== REQUÊTES ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreerCompteRequest {

        @NotBlank(message = "Le nom est obligatoire")
        @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
        private String nom;

        @NotBlank(message = "Le prénom est obligatoire")
        @Size(min = 2, max = 100, message = "Le prénom doit contenir entre 2 et 100 caractères")
        private String prenom;

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format d'email invalide")
        private String email;

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
        private String motDePasse;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperationRequest {

        @NotNull(message = "Le montant est obligatoire")
        @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à 0")
        @Digits(integer = 13, fraction = 2, message = "Le montant doit avoir au maximum 2 décimales")
        private BigDecimal montant;

        @Size(max = 255, message = "La description ne peut pas dépasser 255 caractères")
        private String description;

        // Clé d'idempotence fournie par le client (optionnel, généré si absent)
        private String referenceExterne;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format d'email invalide")
        private String email;

        @NotBlank(message = "Le mot de passe est obligatoire")
        private String motDePasse;
    }

    // ==================== RÉPONSES ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompteResponse {
        private UUID id;
        private String numeroCompte;
        private String nom;
        private String prenom;
        private String email;
        private BigDecimal solde;
        private Compte.StatutCompte statut;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dateCreation;

        public static CompteResponse fromEntity(Compte compte) {
            return CompteResponse.builder()
                    .id(compte.getId())
                    .numeroCompte(compte.getNumeroCompte())
                    .nom(compte.getNom())
                    .prenom(compte.getPrenom())
                    .email(compte.getEmail())
                    .solde(compte.getSolde())
                    .statut(compte.getStatut())
                    .dateCreation(compte.getDateCreation())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionResponse {
        private UUID id;
        private UUID compteId;
        private Transaction.TypeTransaction typeTransaction;
        private BigDecimal montant;
        private BigDecimal soldeAvant;
        private BigDecimal soldeApres;
        private String description;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dateTransaction;

        public static TransactionResponse fromEntity(Transaction tx) {
            return TransactionResponse.builder()
                    .id(tx.getId())
                    .compteId(tx.getCompte().getId())
                    .typeTransaction(tx.getTypeTransaction())
                    .montant(tx.getMontant())
                    .soldeAvant(tx.getSoldeAvant())
                    .soldeApres(tx.getSoldeApres())
                    .description(tx.getDescription())
                    .dateTransaction(tx.getDateTransaction())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperationResponse {
        private String message;
        private BigDecimal soldeAvant;
        private BigDecimal soldeApres;
        private BigDecimal montant;
        private Transaction.TypeTransaction typeTransaction;
        private UUID transactionId;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dateOperation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthResponse {
        private String accessToken;
        private String tokenType;
        private long expiresIn;
        private String email;
        private String role;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErreurResponse {
        private int statut;
        private String erreur;
        private String message;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime timestamp;
    }
}
