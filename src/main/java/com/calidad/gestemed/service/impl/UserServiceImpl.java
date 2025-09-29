package com.calidad.gestemed.service.impl;

import com.calidad.gestemed.domain.User; // Asegúrate que esta sea tu entidad de usuario
import com.calidad.gestemed.domain.Role; // Asegúrate que esta sea tu entidad de rol
import com.calidad.gestemed.repo.UserRepository;
import com.calidad.gestemed.repo.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository; // Repositorio para buscar roles
    private final PasswordEncoder passwordEncoder;

    /*
     * Registra un nuevo usuario en el sistema.
     *
     * @param username El nombre de usuario (debe ser único).
     * @param rawPassword La contraseña en texto plano.
     * @param roleNames Un conjunto de nombres de roles (ej: "LEASING", "TECH").
     * @return El usuario guardado.
     */
    public User registerNewUser(String username, String rawPassword, Set<String> roleNames) {
        // 1. Verificar si el usuario ya existe para evitar duplicados
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("El nombre de usuario ya está en uso: " + username);
        }

        // 2. Buscar las entidades de Rol correspondientes a los nombres
        // los nombres de rol en la BD deben incluir  el prefijo "ROLE_"
        // ej: ROLE_LEASING, ROLE_TECH
        Set<Role> roles = roleNames.stream()
                .map(roleName -> roleRepository.findByName("ROLE_" + roleName)
                        .orElseThrow(() -> new RuntimeException("Error: Rol no encontrado - " + roleName)))
                .collect(Collectors.toSet());

        // 3. Crear una nueva instancia del usuario
        User user = new User();
        user.setUsername(username);
        // 4. Codificar la contraseña antes de guardarla
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRoles(roles);
        user.setEnabled(true); // Activar el usuario por defecto

        // 5. Guardar el usuario en la base de datos
        return userRepository.save(user);
    }
}
