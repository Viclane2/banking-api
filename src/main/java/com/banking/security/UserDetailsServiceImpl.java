package com.banking.security;

import com.banking.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Implémentation de UserDetailsService isolée dans sa propre classe.
 * Casse la dépendance circulaire :
 *   SecurityConfig -> JwtAuthenticationFilter -> UserDetailsService -> SecurityConfig (CYCLE)
 * Devient :
 *   SecurityConfig -> JwtAuthenticationFilter -> UserDetailsServiceImpl (PAS DE CYCLE)
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Utilisateur non trouvé avec l'email : " + email));
    }
}