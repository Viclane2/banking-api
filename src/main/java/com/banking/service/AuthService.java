package com.banking.service;

import com.banking.dto.BankingDtos.*;
import com.banking.entity.Utilisateur;
import com.banking.repository.UtilisateurRepository;
import com.banking.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service d'authentification : inscription et connexion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Inscription d'un nouvel utilisateur (rôle CLIENT par défaut).
     */
    @Transactional
    public AuthResponse inscrire(CreerCompteRequest request) {
        if (utilisateurRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Un utilisateur avec cet email existe déjà : " + request.getEmail());
        }

        Utilisateur utilisateur = Utilisateur.builder()
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(request.getEmail())
                // BCrypt factor 12 — conforme exigence sécurité du CDC
                .motDePasse(passwordEncoder.encode(request.getMotDePasse()))
                .role(Utilisateur.Role.CLIENT)
                .build();

        utilisateurRepository.save(utilisateur);
        log.info("Nouvel utilisateur inscrit : {}", utilisateur.getEmail());

        String token = jwtService.generateToken(utilisateur);

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getJwtExpiration())
                .email(utilisateur.getEmail())
                .role(utilisateur.getRole().name())
                .build();
    }

    /**
     * Connexion d'un utilisateur existant.
     */
    public AuthResponse connecter(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getMotDePasse())
        );

        Utilisateur utilisateur = utilisateurRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        String token = jwtService.generateToken(utilisateur);
        log.info("Connexion réussie : {}", utilisateur.getEmail());

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getJwtExpiration())
                .email(utilisateur.getEmail())
                .role(utilisateur.getRole().name())
                .build();
    }
}
