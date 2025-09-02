package com.calidad.gestemed.service.impl;


import com.calidad.gestemed.domain.RolePolicy;
import com.calidad.gestemed.repo.RolePolicyRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthzService {
    private final RolePolicyRepo repo;

    public boolean has(String role, java.util.function.Predicate<RolePolicy> checker) {
        return repo.findByRoleName(role)
                .map(checker::test)
                .orElse(false);
    }
}