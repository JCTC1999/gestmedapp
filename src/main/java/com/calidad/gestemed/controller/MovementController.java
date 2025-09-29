// controller/MovementController.java
package com.calidad.gestemed.controller;

import com.calidad.gestemed.repo.AssetRepo;
import com.calidad.gestemed.service.MovementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

// Este es el controlador web de Spring MVC para gestionar los movimientos de un activo en específico

@Controller
@RequiredArgsConstructor
@RequestMapping("/assets/{assetId}/movements")
public class MovementController {

    // Dependencias del controlador
    private final MovementService movementService;
    private final AssetRepo assetRepo;

    @GetMapping
    public String list(@PathVariable Long assetId,
                       @RequestParam(required = false) String from,
                       @RequestParam(required = false) String to,
                       @RequestParam(required = false) String location,
                       Model m) {
        var asset = assetRepo.findById(assetId).orElseThrow();

        // Convierte parámetros de fecha (String) a LocalDate. Si están vacíos o null se asigna null
        LocalDate f = (from==null||from.isBlank())? null : LocalDate.parse(from);
        LocalDate t = (to==null||to.isBlank())? null : LocalDate.parse(to);

        m.addAttribute("asset", asset);
        m.addAttribute("items", movementService.history(assetId, f, t, location));
        m.addAttribute("from", from);
        m.addAttribute("to", to);
        m.addAttribute("location", location);
        return "movements/list";
    }

    // Para crear un movimiento de un activo
    @PostMapping
    public String create(@PathVariable Long assetId,
                         @RequestParam String fromLocation,
                         @RequestParam String toLocation,
                         @RequestParam(required=false) String reason,
                         Authentication auth) {
        var asset = assetRepo.findById(assetId).orElseThrow();

        // si hay usuario autenticado, se usa su nombre; si no, se marca como "system"
        String user = (auth!=null? auth.getName() : "system");

        // Se registra el movimiento usando el servicio (incluye toda la lógica: guardar en BD, asignar fechas, etc.)
        movementService.recordMove(asset, fromLocation, toLocation, reason, user);

        return "redirect:/assets/"+assetId+"/movements";
    }
}

