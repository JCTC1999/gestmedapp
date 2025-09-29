package com.calidad.gestemed.controller;

// controller/InventoryController.java

import com.calidad.gestemed.domain.Part;
import com.calidad.gestemed.repo.PartRepo;
import com.calidad.gestemed.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/*
    Controlador para manejar el inventario de piezas
 */


@Controller @RequiredArgsConstructor
@RequestMapping("/inventory")
public class InventoryController {

    //dependencias
    private final PartRepo partRepo;
    private final InventoryService inventoryService;

    // listar las piezas

    @GetMapping public String list(Model m){
        m.addAttribute("parts", partRepo.findAll());
        return "inventory/list";

    }

    // petición get para crear una nueva pieza

    @GetMapping("/new")
    public String form(Model m){
        m.addAttribute("part", new Part());
        return "inventory/new";
    }

    // petición post para crear la pieza y guardarla en la base de datos

    @PostMapping public String create(Part p){
        partRepo.save(p);
        return "redirect:/inventory?created";
    }

    // para registrar la disminución o aumento de las piezas en el inventario
    // delta es el aumento o disminución de la pieza en el inventario
    // id es el id de la pieza
    @PostMapping("/{id}/move")
    public String move(@PathVariable Long id, @RequestParam int delta, @RequestParam String note){
        inventoryService.adjustStock(id, delta, note); return "redirect:/inventory";
    }
}
