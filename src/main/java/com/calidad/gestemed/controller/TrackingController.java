package com.calidad.gestemed.controller;

import com.calidad.gestemed.domain.AssetMovement;
import com.calidad.gestemed.dto.GpsDTO;
import com.calidad.gestemed.repo.AssetMovementRepo;
import com.calidad.gestemed.repo.AssetRepo;
import com.calidad.gestemed.repo.ContractRepo;
import com.calidad.gestemed.service.impl.GeofenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import com.calidad.gestemed.domain.Asset;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;


// controller/TrackingController.java


// controller/TrackingController.java

// Controlador para el rastreo GPS
@Controller
@RequiredArgsConstructor
public class TrackingController {

    private final AssetRepo assetRepo;
    private final ContractRepo contractRepo;
    private final GeofenceService geofenceService;
    private final AssetMovementRepo movementRepo;

    // Página con el mapa y filtros
    @GetMapping("/tracking")
    public String mapPage(Model model,
                          @RequestParam(defaultValue = "10") Integer refreshSeconds) {
        model.addAttribute("refreshSeconds", refreshSeconds);
        model.addAttribute("contracts", contractRepo.findAll());
        model.addAttribute("statuses", contractRepo.distinctStatuses());
        return "tracking/map";
    }

    @GetMapping("/tracking/{id}/move")
    public String moveAsset(@PathVariable Long id, Model model) {
        model.addAttribute("asset", assetRepo.findById(id).orElseThrow());
        return "tracking/movetest";
    }


    // API que devuelve posiciones con filtros
    // su función es servir datos en formato JSON al frontend
    //La anotación ResponseBody indica a Spring que el valor de retorno del método (List<GpsDTO>) debe ser directamente el cuerpo de la respuesta HTTP y no un nombre de vista
    //metodo list puede recibir o no el cliente, el contrato o el status para hacer las consultas
    @GetMapping("/api/gps")
    @ResponseBody
    public List<GpsDTO> list(@RequestParam(required = false) String client,
                             @RequestParam(required = false) Long contractId,
                             @RequestParam(required = false) String status) {

        String clientLike = (client == null || client.isBlank())
                ? null : ("%" + client.toLowerCase() + "%");

        /*
        .stream().map(...).toList(); Estos métodos de Java procesan la lista de resultados.

        .stream(): Convierte la lista en un stream o flujo para procesarla.

        .map(...): Transforma cada objeto Asset (de la base de datos) en un objeto GpsDTO más simple y seguro.

        .toList(): Convierte el resultado final de nuevo en una lista


         */

        // se tranforman los objetos a Asset a objetos GpsDTO

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
    public GpsDTO updateByNumericId(@PathVariable Long assetId,
                                    @RequestParam Double lat,
                                    @RequestParam Double lng,
                                    @RequestParam(required=false) String note,
                                    @RequestParam(required = false) String location,
                                    Principal who) {
        Asset a = assetRepo.findById(assetId).orElseThrow();
        return persistAndNotify(a, lat, lng, note, location, who);
    }

    // por ID de negocio
    @PostMapping("/api/gps/biz/{assetBizId}")
    @ResponseBody
    public GpsDTO updateByBusinessId(@PathVariable String assetBizId,
                                     @RequestParam Double lat,
                                     @RequestParam Double lng,
                                     @RequestParam(required=false) String note,
                                     @RequestParam(required = false) String location,
                                     Principal who) {
        Asset a = assetRepo.findByAssetId(assetBizId).orElseThrow();
        return persistAndNotify(a, lat, lng, note, location, who);
    }

    // guarda última posición y evalúa geocercas
    public GpsDTO persistAndNotify(Asset a, Double lat, Double lng, String note, String location, Principal who) {
        if (lat == null || lng == null) throw new IllegalArgumentException("lat/lng requeridos");

        String from = a.getInitialLocation();
        Double fromLatitude = a.getLastLatitude();
        Double fromLongitude = a.getLastLongitude();


        a.setLastLatitude(lat);
        a.setLastLongitude(lng);
        a.setInitialLocation(location);

        a.setLastGpsAt(LocalDateTime.now());

        // OJO: el campo en la entidad es 'reason' (no 'note')
        movementRepo.save(AssetMovement.builder()
                .asset(a)
                .fromLocation(from)
                .toLocation(location)
                .fromLocationLatitude(fromLatitude)
                .fromLocationLongitude(fromLongitude)
                .toLocationLatitude(lat)
                .toLocationLongitude(lng)
                .reason(note) // guarda nota en 'reason'
                .performedBy(who!=null?who.getName():"system")
                .movedAt(LocalDateTime.now())
                .build());


        assetRepo.save(a);

        // Disparar evaluación de geocercas
        geofenceService.processPosition(a, lat, lng);

        return new GpsDTO(a.getId(), a.getAssetId(), a.getModel(),
                a.getLastLatitude(), a.getLastLongitude(), a.getLastGpsAt());
    }

}

