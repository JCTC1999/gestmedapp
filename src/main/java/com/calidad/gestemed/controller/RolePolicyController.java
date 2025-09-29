
package com.calidad.gestemed.controller;

import com.calidad.gestemed.domain.Role;
import com.calidad.gestemed.domain.RolePolicy;
import com.calidad.gestemed.repo.RolePolicyRepo;
import com.calidad.gestemed.repo.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

@Controller
@RequestMapping("/admin/roles")
@RequiredArgsConstructor
public class RolePolicyController {

    private final RolePolicyRepo policyRepo;
    private final RoleRepository roleRepo;

    @GetMapping
    public String list(Model m) {
        m.addAttribute("roles", policyRepo.findAll());
        return "roles/list";
    }

    @GetMapping("/new")
    public String form(Model m) {
        m.addAttribute("role", new RolePolicy());
        return "roles/edit";
    }

    @GetMapping("/{id}")
    public String edit(@PathVariable Long id, Model m) {
        m.addAttribute("role", policyRepo.findById(id).orElseThrow());
        return "roles/edit";
    }

    @PostMapping
    public String save(@ModelAttribute("role") RolePolicy policy) {
        // Guardamos la política de permisos (ej: 'SUPPORT')
        policyRepo.save(policy);

        // Construimos el nombre del rol de autenticación (ej: 'ROLE_SUPPORT')
        String roleNameForAuth = "ROLE_" + policy.getRoleName().toUpperCase();

        // Verificamos si un rol con ese nombre ya existe en la tabla 'roles'
        if (roleRepo.findByName(roleNameForAuth).isEmpty()) {
            // Si no existe, lo creamos y lo guardamos.
            Role newAuthRole = Role.builder()
                    .name(roleNameForAuth)
                    .build();
            roleRepo.save(newAuthRole);
        }

        return "redirect:/admin/roles";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        policyRepo.deleteById(id);
        return "redirect:/admin/roles";
    }
}