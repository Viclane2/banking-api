package com.banking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Entité représentant un compte bancaire.
 * Utilise BigDecimal pour garantir la précision des calculs financiers.
 */
@Entity
@Table(name = "comptes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Compte {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "numero_compte", unique = true, nullable = false, length = 20)
    private String numeroCompte;

    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Column(name = "prenom", nullable = false, length = 100)
    private String prenom;

    @Column(name = "email", unique = true, nullable = false, length = 255)
    private String email;

    /**
     * Solde du compte - TOUJOURS BigDecimal pour éviter les erreurs de virgule flottante.
     * precision=15 : jusqu'à 15 chiffres au total
     * scale=2     : exactement 2 décimales
     */
    @Column(name = "solde", nullable = false, precision = 15, scale = 2)
    private BigDecimal solde;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutCompte statut;

    @CreatedDate
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @LastModifiedDate
    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @OneToMany(mappedBy = "compte", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> transactions;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    public enum StatutCompte {
        ACTIF, INACTIF, SUSPENDU
    }
}
