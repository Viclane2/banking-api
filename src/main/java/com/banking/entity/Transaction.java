package com.banking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité représentant une transaction bancaire (dépôt ou retrait).
 * Chaque transaction est immuable une fois créée (journalisation audit).
 */
@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_id", nullable = false)
    private Compte compte;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_transaction", nullable = false)
    private TypeTransaction typeTransaction;

    @Column(name = "montant", nullable = false, precision = 15, scale = 2)
    private BigDecimal montant;

    @Column(name = "solde_avant", nullable = false, precision = 15, scale = 2)
    private BigDecimal soldeAvant;

    @Column(name = "solde_apres", nullable = false, precision = 15, scale = 2)
    private BigDecimal soldeApres;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "reference_externe", unique = true, length = 50)
    private String referenceExterne; // Pour l'idempotence

    @CreatedDate
    @Column(name = "date_transaction", nullable = false, updatable = false)
    private LocalDateTime dateTransaction;

    public enum TypeTransaction {
        DEPOT, RETRAIT
    }
}
