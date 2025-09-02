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

@Controller
@RequiredArgsConstructor
@RequestMapping("/assets/{assetId}/movements")
public class MovementController {

    private final MovementService movementService;
    private final AssetRepo assetRepo;

    @GetMapping
    public String list(@PathVariable Long assetId,
                       @RequestParam(required = false) String from,
                       @RequestParam(required = false) String to,
                       @RequestParam(required = false) String location,
                       Model m) {
        var asset = assetRepo.findById(assetId).orElseThrow();
        LocalDate f = (from==null||from.isBlank())? null : LocalDate.parse(from);
        LocalDate t = (to==null||to.isBlank())? null : LocalDate.parse(to);

        m.addAttribute("asset", asset);
        m.addAttribute("items", movementService.history(assetId, f, t, location));
        m.addAttribute("from", from);
        m.addAttribute("to", to);
        m.addAttribute("location", location);
        return "movements/list";
    }

    /** Form opcional para asentar un movimiento manual (auditor o admin). */
    @PostMapping
    public String create(@PathVariable Long assetId,
                         @RequestParam String fromLocation,
                         @RequestParam String toLocation,
                         @RequestParam(required=false) String reason,
                         Authentication auth) {
        var asset = assetRepo.findById(assetId).orElseThrow();
        String user = (auth!=null? auth.getName() : "system");
        movementService.recordMove(asset, fromLocation, toLocation, reason, user);
        // también podrías actualizar asset.initialLocation = toLocation si aplica
        return "redirect:/assets/"+assetId+"/movements";
    }
}
