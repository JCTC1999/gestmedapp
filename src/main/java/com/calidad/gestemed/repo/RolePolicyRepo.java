package com.calidad.gestemed.repo;

import com.calidad.gestemed.domain.RolePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolePolicyRepo extends JpaRepository<RolePolicy, Long> {
    Optional<RolePolicy> findByRoleName(String roleName);
}

