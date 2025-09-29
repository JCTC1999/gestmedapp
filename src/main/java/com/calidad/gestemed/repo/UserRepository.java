package com.calidad.gestemed.repo;
import com.calidad.gestemed.domain.User; // Asegúrate de importar tu clase User
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Spring Data JPA creará la consulta automáticamente por el nombre del método
    Optional<User> findByUsername(String username);
}
