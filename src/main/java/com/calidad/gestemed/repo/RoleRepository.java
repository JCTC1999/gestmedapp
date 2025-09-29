package com.calidad.gestemed.repo;

import com.calidad.gestemed.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {

    /*
     * Busca un rol por su nombre exacto.
     * Spring Data JPA crea la consulta automáticamente.
     * @param name El nombre del rol (ej: "ROLE_ADMIN").
     * @return Un Optional que contiene el rol si se encuentra.
     */
    Optional<Role> findByName(String name);
}
