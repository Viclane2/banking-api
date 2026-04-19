package com.banking.repository;

import com.banking.entity.Compte;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompteRepository extends JpaRepository<Compte, UUID> {

    Optional<Compte> findByNumeroCompte(String numeroCompte);

    boolean existsByEmail(String email);

    List<Compte> findByUtilisateurId(UUID utilisateurId);

    Page<Compte> findByStatut(Compte.StatutCompte statut, Pageable pageable);

    @Query("SELECT c FROM Compte c WHERE c.utilisateur.id = :utilisateurId AND c.statut = 'ACTIF'")
    List<Compte> findComptesActifsByUtilisateur(UUID utilisateurId);
}
