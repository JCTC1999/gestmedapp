package com.calidad.gestemed.controller;
// controller/ContractController.java

import com.calidad.gestemed.domain.Asset;
import com.calidad.gestemed.domain.Contract;
import com.calidad.gestemed.repo.AssetRepo;
import com.calidad.gestemed.repo.ContractRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.HashSet;

/*
    Controlador para gestionar los contratos
 */


@Controller @RequiredArgsConstructor
@RequestMapping("/contracts")
public class ContractController {

    // Dependencias del controlador

    private final ContractRepo contractRepo;
    private final AssetRepo assetRepo;

    //listar los contratos

    @GetMapping public String list(Model model){ model.addAttribute("contracts", contractRepo.findAll()); return "contracts/list"; }

    // petición get para crear un contrato nuevo

    @GetMapping("/new") public String form(Model model){
        model.addAttribute("contract", new Contract());
        model.addAttribute("assets", assetRepo.findAll());
        return "contracts/new";
    }

    // petición post para crear el contrato
    // recibe un arreglo de assetIds ya que un contrato puede tener asociados muchos activos
    @PostMapping
    public String create(Contract c, @RequestParam(required=false) Long[] assetIds){
        //@RequestParam(required=false) Long[] assetIds: es un parámetro opcional del formulario con un conjunto de IDs de activos (assets). Puede venir vacío (null).

        //Inicializar los assets del contrato. Se asegura de que el contrato empiece con un conjunto vacío de assets.
        c.setAssets(new HashSet<>());

        if (assetIds!=null)
            for(Long id: assetIds)
                c.getAssets().add(assetRepo.findById(id).orElseThrow());


        contractRepo.save(c);

        //El ?created es solo un truco para pasar información a la siguiente vista sin usar sesión ni nada complicado.
        return "redirect:/contracts?created";
    }
}
