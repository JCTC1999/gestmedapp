package com.calidad.gestemed.controller;

import com.calidad.gestemed.domain.Asset;
import com.calidad.gestemed.domain.AssetMovement;
import com.calidad.gestemed.repo.AssetMovementRepo;
import com.calidad.gestemed.repo.AssetRepo;
import com.calidad.gestemed.service.AssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/assets")
public class AssetController {
    private final AssetService assetService;
    private final AssetMovementRepo movementRepo;
    private final AssetRepo assetRepo;

    @GetMapping
    public String list(Model model){
        model.addAttribute("assets", assetService.list());
        return "assets/list";
    }

    @GetMapping("/new")
    public String form(Model model){
        model.addAttribute("asset", new Asset());
        return "assets/new";
    }

    @PostMapping
    public String create(Asset asset, @RequestParam("photos") List<MultipartFile> photos, Authentication auth){
        assetService.create(asset, photos, (auth!=null?auth.getName():"admin"));
        return "redirect:/assets?created";
    }

    /** Atajo: redirige al historial filtrable */
    @GetMapping("/{id}/movements")
    public String movements(@PathVariable Long id){
        return "redirect:/assets/" + id + "/history";
    }

    @GetMapping("/{id}/move")
    public String moveForm(@PathVariable Long id, Model model) {
        model.addAttribute("asset", assetRepo.findById(id).orElseThrow());
        return "assets/move";
    }

    @PostMapping("/{id}/move")
    public String doMove(@PathVariable Long id,
                         @RequestParam String toLocation,
                         @RequestParam Double toLocationLatitude,
                         @RequestParam Double toLocationLongitude,
                         @RequestParam(required=false) String note,
                         Principal who) {
        Asset a = assetRepo.findById(id).orElseThrow();
        String from = a.getInitialLocation();
        Double fromLatitude = a.getLastLatitude();
        Double fromLongitude = a.getLastLongitude();

        a.setInitialLocation(toLocation);
        a.setLastLatitude(fromLatitude);
        a.setLastLongitude(fromLongitude);


        assetRepo.save(a);

        // OJO: el campo en la entidad es 'reason' (no 'note')
        movementRepo.save(AssetMovement.builder()
                .asset(a)
                .fromLocation(from)
                .toLocation(toLocation)
                        .fromLocationLatitude(fromLatitude)
                        .fromLocationLongitude(fromLongitude)
                        .toLocationLatitude(toLocationLatitude)
                        .toLocationLongitude(toLocationLongitude)
                .reason(note) // guarda tu nota en 'reason'
                .performedBy(who!=null?who.getName():"system")
                .movedAt(LocalDateTime.now())
                .build());

        return "redirect:/assets/" + id + "/history";
    }

    @GetMapping("/{id}/history")
    public String history(@PathVariable Long id,
                          @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
                          @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
                          @RequestParam(required=false) String location,
                          Model model) {

        // Normalizamos a rangos [from, to) para el query
        LocalDateTime f = (from==null? null: from.atStartOfDay());
        LocalDateTime t = (to==null? null: to.plusDays(1).atStartOfDay());
        String loc = (location==null || location.isBlank()) ? null : location.trim();

        var asset = assetRepo.findById(id).orElseThrow();
        var list = movementRepo.search(id, f, t, loc);  // usa el @Query del repo

        model.addAttribute("asset", asset);
        model.addAttribute("movs", list);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("location", location);

        return "assets/history";
    }
}
