package com.calidad.gestemed.controller;

import com.calidad.gestemed.dto.GpsDTO;
import com.calidad.gestemed.repo.AssetRepo;
import com.calidad.gestemed.repo.ContractRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import com.calidad.gestemed.domain.Asset;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;


// controller/TrackingController.java
@Controller
@RequiredArgsConstructor
public class TrackingController {

    private final AssetRepo assetRepo;
    private final ContractRepo contractRepo;

    // Página con el mapa y filtros
    @GetMapping("/tracking")
    public String mapPage(Model model,
                          @RequestParam(defaultValue = "10") Integer refreshSeconds) {
        model.addAttribute("refreshSeconds", refreshSeconds);
        model.addAttribute("contracts", contractRepo.findAll());
        model.addAttribute("statuses", contractRepo.distinctStatuses());
        return "tracking/map";
    }

    // API que devuelve posiciones con filtros
    @GetMapping("/api/gps")
    @ResponseBody
    public List<GpsDTO> list(@RequestParam(required = false) String client,
                             @RequestParam(required = false) Long contractId,
                             @RequestParam(required = false) String status) {

        String clientLike = (client == null || client.isBlank())
                ? null : ("%" + client.toLowerCase() + "%");

        return assetRepo.findForGps(clientLike, contractId, status)
                .stream()
                .map(a -> new GpsDTO(
                        a.getId(),
                        a.getAssetId(),
                        a.getModel(),
                        a.getLastLatitude(),
                        a.getLastLongitude(),
                        a.getLastGpsAt()
                ))
                .toList();
    }

    // Endpoint para simular/actualizar la posición de un activo
    @PostMapping("/api/gps/{assetId}")
    @ResponseBody
    public GpsDTO update(@PathVariable Long assetId,
                         @RequestParam Double lat,
                         @RequestParam Double lng) {
        Asset a = assetRepo.findById(assetId).orElseThrow();
        a.setLastLatitude(lat);
        a.setLastLongitude(lng);
        a.setLastGpsAt(LocalDateTime.now());
        assetRepo.save(a);
        return new GpsDTO(a.getId(), a.getAssetId(), a.getModel(),
                a.getLastLatitude(), a.getLastLongitude(), a.getLastGpsAt());
    }
}

