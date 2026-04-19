package com.banking.repository;

import com.banking.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Page<Transaction> findByCompteIdOrderByDateTransactionDesc(UUID compteId, Pageable pageable);

    // Pour garantir l'idempotence : vérifier si une référence externe existe déjà
    Optional<Transaction> findByReferenceExterne(String referenceExterne);

    boolean existsByReferenceExterne(String referenceExterne);
}
