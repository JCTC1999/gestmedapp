package com.calidad.gestemed.controller;

// controller/InventoryController.java

import com.calidad.gestemed.domain.Part;
import com.calidad.gestemed.repo.PartRepo;
import com.calidad.gestemed.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller @RequiredArgsConstructor
@RequestMapping("/inventory")
public class InventoryController {
    private final PartRepo partRepo;
    private final InventoryService inventoryService;

    @GetMapping public String list(Model m){
        m.addAttribute("parts", partRepo.findAll());
        return "inventory/list";

    }

    @GetMapping("/new")
    public String form(Model m){
        m.addAttribute("part", new Part());
        return "inventory/new";
    }



    @PostMapping public String create(Part p){
        partRepo.save(p);
        return "redirect:/inventory?created";
    }


    @PostMapping("/{id}/move")
    public String move(@PathVariable Long id, @RequestParam int delta, @RequestParam String note){
        inventoryService.adjustStock(id, delta, note); return "redirect:/inventory";
    }
}
