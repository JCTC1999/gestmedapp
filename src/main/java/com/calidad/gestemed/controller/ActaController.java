package com.calidad.gestemed.controller;

import com.calidad.gestemed.domain.Acta;
import com.calidad.gestemed.repo.ActaRepo;
import com.calidad.gestemed.repo.AssetRepo;
import com.calidad.gestemed.repo.ContractRepo;
import com.calidad.gestemed.service.impl.AzureBlobService;
import com.calidad.gestemed.util.ByteArrayMultipartFile;
import com.calidad.gestemed.dto.ActaForm;
import com.calidad.gestemed.dto.ActaSignForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;

/*
    Clase para las actas
 */
@Controller
@RequestMapping("/acts")
@RequiredArgsConstructor
public class ActaController {

    private final ActaRepo actaRepo;
    private final AssetRepo assetRepo;
    private final ContractRepo contractRepo;
    private final AzureBlobService azureBlobService;

    // Formulario vacío: el usuario selecciona manualmente (asset, tipo, contrato, notas)
    @GetMapping("/new")
    public String newForm(Model m) {
        m.addAttribute("form", new ActaForm());
        m.addAttribute("assets", assetRepo.findAll());
        m.addAttribute("contracts", contractRepo.findAll());
        m.addAttribute("types", Acta.Tipo.values());
        return "acts/new"; // nueva vista
    }

    // Crea el Acta con datos del formulario (sin query params).
    @PostMapping
    public String create(@ModelAttribute("form") ActaForm form, Principal who) {
        var asset = assetRepo.findById(form.getAssetId()).orElseThrow();

        var builder = Acta.builder()
                .asset(asset)
                .tipo(form.getType())
                .createdAt(LocalDateTime.now())
                .createdBy(who.getName())
                .notes(form.getNotes());

        if (form.getContractId() != null) {
            contractRepo.findById(form.getContractId()).ifPresent(builder::contract);
        }

        var acta = builder.build();
        actaRepo.save(acta);
        return "redirect:/acts/" + acta.getId();
    }

    // Pantalla de detalle / firma (canvas).
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model m) {
        m.addAttribute("acta", actaRepo.findById(id).orElseThrow());
        m.addAttribute("signForm", new ActaSignForm()); // para post de firma
        return "acts/sign"; // vista de firma
    }

    // Firma vía formulario (sin request params en URL).
    @PostMapping("/{id}/sign")
    public String sign(@PathVariable Long id,
                       @ModelAttribute("signForm") ActaSignForm form) throws IOException {

        var acta = actaRepo.findById(id).orElseThrow();

        // Firma: dataURL -> bytes -> MultipartFile -> Azure
        if (form.getSignatureDataUrl() != null && form.getSignatureDataUrl().startsWith("data:image")) {
            String b64 = form.getSignatureDataUrl().substring(form.getSignatureDataUrl().indexOf(",") + 1);
            byte[] png = java.util.Base64.getDecoder().decode(b64);
            MultipartFile f = new ByteArrayMultipartFile(png, "signature.png", "image/png");
            String url = azureBlobService.uploadFile(f);
            acta.setSignaturePath(url);
        }

        // Ubicación: manual si la escriben o caemos al último GPS del activo
        Double lat = form.getLat();
        Double lng = form.getLng();
        if (lat == null || lng == null) {
            var a = acta.getAsset();
            if (lat == null) lat = a.getLastLatitude();
            if (lng == null) lng = a.getLastLongitude();
        }

        acta.setSignatureLat(lat);
        acta.setSignatureLng(lng);
        acta.setSignedAt(LocalDateTime.now());
        actaRepo.save(acta);

        return "redirect:/acts/" + id;
    }

    @GetMapping
    public String list(Model m) {
        m.addAttribute("actas", actaRepo.findAll()); // Busca todas las actas en la BD
        return "acts/list"; // Envía los datos a la nueva vista "list.html"
    }

}