package com.calidad.gestemed.controller;

import com.calidad.gestemed.domain.RolePolicy;
import com.calidad.gestemed.repo.RolePolicyRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

// controller/RolePolicyController.java
@Controller
@RequestMapping("/admin/roles") @RequiredArgsConstructor
public class RolePolicyController {
    private final RolePolicyRepo repo;

    @GetMapping
    public String list(Model m) { m.addAttribute("roles", repo.findAll()); return "roles/list"; }

    @GetMapping("/new")
    public String form(Model m) { m.addAttribute("role", new RolePolicy()); return "roles/edit"; }

    @GetMapping("/{id}")
    public String edit(@PathVariable Long id, Model m) {
        m.addAttribute("role", repo.findById(id).orElseThrow());
        return "roles/edit";
    }

    @PostMapping
    public String save(@ModelAttribute("role") RolePolicy p) {
        repo.save(p); return "redirect:/admin/roles";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        repo.deleteById(id); return "redirect:/admin/roles";
    }
}
