package com.calidad.gestemed.service.impl;

import com.calidad.gestemed.repo.UserRepository; // Importa tu repositorio
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Buscar el usuario en la base de datos
        com.calidad.gestemed.domain.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No se encontró el usuario: " + username));

        // 2. Mapear los roles del usuario a la colección de GrantedAuthority que Spring Security necesita
        Collection<? extends GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());

        // 3. Devolver un objeto UserDetails (la implementación de Spring)
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isEnabled(),
                true,
                true,
                true,
                authorities
        );
    }
}