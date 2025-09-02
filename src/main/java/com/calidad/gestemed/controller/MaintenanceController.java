package com.calidad.gestemed.controller;

// controller/MaintenanceController.java

import com.calidad.gestemed.domain.*;
import com.calidad.gestemed.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
//import org.springframework.util.Base64Utils;
import java.io.InputStream;
import java.util.Base64;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Controller @RequiredArgsConstructor
@RequestMapping("/maintenance")
public class MaintenanceController {
    private final MaintenanceOrderRepo orderRepo;
    private final AssetRepo assetRepo;
    private final PartRepo partRepo;
    private final PartConsumptionRepo consRepo;

    @GetMapping("/new")
    public String formNew(@RequestParam Long assetId, Model model) {
        model.addAttribute("asset", assetRepo.findById(assetId).orElseThrow());
        return "maintenance/new";
    }

    @GetMapping("/list")
    public String show(Model model) {
        var all = orderRepo.findAll(); //
        var pendientes  = all.stream().filter(o -> "PENDIENTE".equals(o.getStatus().toString())).toList();
        var enCurso     = all.stream().filter(o -> "EN_CURSO".equals(o.getStatus().toString())).toList();
        var finalizadas = all.stream().filter(o -> {
            var s = o.getStatus().toString();
            return "FINALIZADO".equals(s) || "CERRADA".equals(s);
        }).toList();

        model.addAttribute("pendientes", pendientes);
        model.addAttribute("enCurso", enCurso);
        model.addAttribute("finalizadas", finalizadas);
        return "maintenance/show";
    }

    @GetMapping("/showMaintenanceByAsset/{assetId}")
    public String showByAsset(@PathVariable Long assetId, Model model) {
        var asset = assetRepo.findById(assetId).orElseThrow(); //
        var orders = orderRepo.findByAsset(asset);


        var pendientes  = orders.stream().filter(o -> "PENDIENTE".equals(o.getStatus().toString())).toList();
        var enCurso     = orders.stream().filter(o -> "EN_CURSO".equals(o.getStatus().toString())).toList();
        var finalizadas = orders.stream().filter(o -> {
            var s = o.getStatus().toString();
            return "FINALIZADO".equals(s) || "CERRADA".equals(s);
        }).toList();

        model.addAttribute("pendientes", pendientes);
        model.addAttribute("enCurso", enCurso);
        model.addAttribute("finalizadas", finalizadas);
        return "maintenance/showmaintenancebyasset";
    }


    //menos control no poner la direccion por ejemplo /create
    //dificulta la lectura
    @PostMapping
    public String create(@RequestParam Long assetId, @RequestParam MaintType type,
                         @RequestParam String responsible, @RequestParam String tasks,
                         @RequestParam String date) {
        MaintenanceOrder o = MaintenanceOrder.builder()
                .asset(assetRepo.findById(assetId).orElseThrow())
                .type(type).responsible(responsible).tasks(tasks)
                .scheduledDate(LocalDate.parse(date))
                .status(MaintStatus.PENDIENTE).build();
        orderRepo.save(o);
        return "redirect:/maintenance/" + o.getId();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderRepo.findById(id).orElseThrow());
        model.addAttribute("parts", partRepo.findAll());
        return "maintenance/detail";
    }

    @GetMapping("showCompleted/{id}")
    public String showcompleted(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderRepo.findById(id).orElseThrow());
        return "maintenance/showcompleted";
    }

    @PostMapping("/{id}/consume")
    public String consume(@PathVariable Long id, @RequestParam Long partId, @RequestParam Integer qty) {
        MaintenanceOrder o = orderRepo.findById(id).orElseThrow();
        Part p = partRepo.findById(partId).orElseThrow();
        consRepo.save(PartConsumption.builder().orderRef(o).part(p).quantity(qty).build());
        p.setStock(p.getStock()-qty);
        partRepo.save(p);
        return "redirect:/maintenance/" + id;
    }

    @PostMapping("/maintenance/{id}/start")
    public String start(@PathVariable Long id) {
        var o = orderRepo.findById(id).orElseThrow();
        o.setStatus(MaintStatus.EN_CURSO);
        orderRepo.save(o);
        return "redirect:/maintenance/"+id;
    }


    @PostMapping("/{id}/close")
    public String close(@PathVariable Long id,
                        @RequestParam(value = "photos", required = false) MultipartFile[] photos,
                        @RequestParam(value = "signatureDataUrl", required = false) String signatureDataUrl,
                        Model model) {

        var order = orderRepo.findById(id).orElseThrow();

        // Carpeta destino: uploads/maintenance/{id}/
        String baseDir = System.getProperty("user.dir");
        Path orderDir = Paths.get(baseDir, "uploads", "maintenance", String.valueOf(id));
        try {
            Files.createDirectories(orderDir);
        } catch (Exception e) {
            model.addAttribute("error", "No se pudo crear carpeta de evidencias: " + e.getMessage());
            model.addAttribute("order", order);
            return "maintenance/detail";
        }

        // 2.1) Guardar FOTOS múltiples
        if (photos != null) {
            for (MultipartFile f : photos) {
                if (f != null && !f.isEmpty()) {
                    String clean = sanitize(f.getOriginalFilename());
                    if (clean == null || clean.isBlank()) clean = "evidencia_" + System.currentTimeMillis() + ".bin";
                    String filename = System.currentTimeMillis() + "_" + clean;
                    Path dst = orderDir.resolve(filename);
                    try (InputStream in = f.getInputStream()) {
                        Files.copy(in, dst, StandardCopyOption.REPLACE_EXISTING);
                        // Ruta relativa servible por HTTP
                        String rel = Paths.get("maintenance", String.valueOf(id), filename).toString().replace("\\","/");
                        order.setPhotoPaths(appendPath(order.getPhotoPaths(), rel));  // <-- acumulamos en String
                    } catch (Exception ex) {
                        System.out.println("[WARN] No se pudo guardar foto: " + ex.getMessage());
                    }
                }
            }
        }

        // 2.2) Guardar FIRMA (data URL base64)
        if (signatureDataUrl != null && signatureDataUrl.startsWith("data:image")) {
            try {
                String b64 = signatureDataUrl.substring(signatureDataUrl.indexOf(",") + 1);
                byte[] png = Base64.getDecoder().decode(b64);
                String fileName = "signature_" + System.currentTimeMillis() + ".png";
                Path dst = orderDir.resolve(fileName);
                Files.write(dst, png);
                String rel = Paths.get("maintenance", String.valueOf(id), fileName).toString().replace("\\","/");
                order.setSignaturePath(rel);
            } catch (Exception e) {
                System.out.println("[WARN] No se pudo guardar firma: " + e.getMessage());
            }
        }

        // 2.3) Cerrar orden
        order.setStatus(MaintStatus.FINALIZADO);   // usa tu enum actual
        order.setClosedAt(LocalDateTime.now());
        orderRepo.save(order);

        return "redirect:/maintenance/" + id;
    }

    // Helpers
    private String sanitize(String name) {
        if (name == null) return null;
        String s = name.replaceAll("[\\\\/]+", "_");
        s = s.replaceAll("[^a-zA-Z0-9._-]", "_");
        return s;
    }

    private String appendPath(String existing, String rel) {
        if (existing == null || existing.isBlank()) return rel;
        // usamos '|' como separador
        return existing + "|" + rel;
    }
}
