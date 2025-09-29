// controller/GeofenceController.java
package com.calidad.gestemed.controller;

import com.calidad.gestemed.domain.Asset;
import com.calidad.gestemed.domain.Geofence;
import com.calidad.gestemed.repo.AssetRepo;
import com.calidad.gestemed.repo.GeofenceRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;

/*
    Controlador para la geocerca
 */
@Controller @RequiredArgsConstructor
public class GeofenceController {

    //dependencias
    private final GeofenceRepo geofenceRepo;
    private final AssetRepo assetRepo;

    //mostrar la geofences
    @GetMapping("/geofences")
    public String page(Model model) {
        model.addAttribute("geofences", geofenceRepo.findAll());
        model.addAttribute("assets", assetRepo.findAll());
        return "geofences/index";
    }

    @GetMapping("/geofences/test")
    public String test(Model model) {
        return "geofences/test";
    }

    // Alta rápida desde el mapa
    @PostMapping("/api/geofences")
    @ResponseBody
    public Geofence create(@RequestParam String name,
                           @RequestParam String polygonJson,
                           @RequestParam(required=false) List<Long> assetIds,
                           @RequestParam(defaultValue="true") boolean active,
                           @RequestParam(defaultValue="true") boolean alertOnEnter,
                           @RequestParam(defaultValue="true") boolean alertOnExit) {

        var g = Geofence.builder()
                .name(name)
                .polygonJson(polygonJson)
                .active(active)
                .alertOnEnter(alertOnEnter)
                .alertOnExit(alertOnExit)
                .build();

        if (assetIds != null && !assetIds.isEmpty()) {
            g.setAssets(new HashSet<>(assetRepo.findAllById(assetIds)));
        }
        return geofenceRepo.save(g);
    }

    //activar o desactivar la geofence
    @PostMapping("/api/geofences/{id}/toggle")
    @ResponseBody
    public Geofence toggle(@PathVariable Long id, @RequestParam boolean active) {
        var g = geofenceRepo.findById(id).orElseThrow();
        g.setActive(active);
        return geofenceRepo.save(g);
    }
}