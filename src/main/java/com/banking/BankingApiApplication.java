package com.banking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Point d'entrée principal de l'API Bancaire Sécurisée.
 * 
 * Fonctionnalités exposées :
 *  - Gestion des comptes bancaires (création, consultation)
 *  - Opérations de dépôt et retrait
 *  - Authentification JWT
 */
@SpringBootApplication
@EnableJpaAuditing
public class BankingApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankingApiApplication.class, args);
    }
}
