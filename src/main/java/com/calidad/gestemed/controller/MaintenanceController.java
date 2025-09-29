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

import com.calidad.gestemed.domain.*;
import com.calidad.gestemed.repo.*;
import com.calidad.gestemed.service.impl.AzureBlobService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

// Este es el controlador web de Spring MVC para gestionar órdenes de mantenimiento de activos médicos


@Controller @RequiredArgsConstructor
@RequestMapping("/maintenance")
public class MaintenanceController {

    // Dependencias del controlador (inyectadas por Spring gracias a Lombok y @RequiredArgsConstructor)

    private final MaintenanceOrderRepo orderRepo;
    private final AssetRepo assetRepo;
    private final PartRepo partRepo;
    private final PartConsumptionRepo consRepo;
    private final AzureBlobService azureBlobService;

    //muestra el formulario para crear una nueva orden de mantenimiento para un activo específico
    @GetMapping("/new")
    public String formNew(@RequestParam Long assetId, Model model) {
        model.addAttribute("asset", assetRepo.findById(assetId).orElseThrow());
        return "maintenance/new";
    }

    //muestra todas las órdenes separadas por estado (pendientes, en curso, finalizadas/cerradas)
    @GetMapping("/list")
    public String show(Model model) {
        var all = orderRepo.findAll(); // obtiene todas las órdenes de mantenimiento

        // Separa por estado con filtros. Se usa toString() del enum de estado para comparar con cadenas.
        // Nota: sería más robusto comparar directamente con el enum (o.equals(MaintStatus.PENDIENTE)) en lugar de usar toString()
        var pendientes  = all.stream().filter(o -> "PENDIENTE".equals(o.getStatus().toString())).toList();
        var enCurso     = all.stream().filter(o -> "EN_CURSO".equals(o.getStatus().toString())).toList();
        var finalizadas = all.stream().filter(o -> {
            var s = o.getStatus().toString();
            return "FINALIZADO".equals(s) || "CERRADA".equals(s);
        }).toList();

        // Pasa las listas por estado a la vista

        model.addAttribute("pendientes", pendientes);
        model.addAttribute("enCurso", enCurso);
        model.addAttribute("finalizadas", finalizadas);
        return "maintenance/show";
    }

    //lista de órdenes por activo, separadas por estado
    @GetMapping("/showMaintenanceByAsset/{assetId}")
    public String showByAsset(@PathVariable Long assetId, Model model) {
        var asset = assetRepo.findById(assetId).orElseThrow();  // obtiene el activo o lanza excepción si no existe
        var orders = orderRepo.findByAsset(asset);// trae solo las órdenes asociadas a ese activo

        // Separa por estado (mismo criterio que /list)
        var pendientes  = orders.stream().filter(o -> "PENDIENTE".equals(o.getStatus().toString())).toList();
        var enCurso     = orders.stream().filter(o -> "EN_CURSO".equals(o.getStatus().toString())).toList();
        var finalizadas = orders.stream().filter(o -> {
            var s = o.getStatus().toString();
            return "FINALIZADO".equals(s) || "CERRADA".equals(s);
        }).toList();

        // Envía a la vista las listas por estado y el activo actual (para encabezados o breadcrumbs)

        model.addAttribute("pendientes", pendientes);
        model.addAttribute("enCurso", enCurso);
        model.addAttribute("finalizadas", finalizadas);
        model.addAttribute("asset", asset);
        return "maintenance/showmaintenancebyasset";
    }


    // POST /maintenance -> crea una nueva orden de mantenimiento (sin map específico: menor control de URL, pero más corto)
    // menos control no poner la direccion por ejemplo /create
    // dificulta la lectura
    @PostMapping
    public String create(@RequestParam Long assetId, @RequestParam MaintType type,
                         @RequestParam String responsible, @RequestParam String tasks,
                         @RequestParam String date) {

        // Construye la orden usando el patrón builder: más legible al setear muchos campos

        MaintenanceOrder o = MaintenanceOrder.builder()
                .asset(assetRepo.findById(assetId).orElseThrow()) // referencia al activo
                .type(type)                                       // tipo de mantenimiento (enum)
                .responsible(responsible) // responsable
                .tasks(tasks) // tareas a realizar (texto)
                .scheduledDate(LocalDate.parse(date)) // fecha programada (parsea el String a LocalDate)
                .status(MaintStatus.PENDIENTE)  // estado inicial: PENDIENTE
                .build();
        orderRepo.save(o); // guarda la orden en la base de datos
        return "redirect:/maintenance/" + o.getId(); // redirige al detalle de la orden recién creada
    }

    //muestra el detalle de una orden y el catálogo de repuestos para consumir
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderRepo.findById(id).orElseThrow()); // orden actual
        model.addAttribute("parts", partRepo.findAll()); // listado de repuestos disponibles
        return "maintenance/detail";  // muestra la vista maintenance/detail.html
    }

    //vista de una orden finalizada (solo lectura)
    @GetMapping("showCompleted/{id}")
    public String showcompleted(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderRepo.findById(id).orElseThrow());
        return "maintenance/showcompleted";
    }

    //registra consumo de repuestos para una orden
    @PostMapping("/{id}/consume")
    public String consume(@PathVariable Long id, @RequestParam Long partId, @RequestParam Integer qty) {
        MaintenanceOrder o = orderRepo.findById(id).orElseThrow(); // orden objetivo
        Part p = partRepo.findById(partId).orElseThrow(); // repuesto a consumir

        // Registra el consumo (cuántas piezas se usan para esta orden)

        consRepo.save(PartConsumption.builder().orderRef(o).part(p).quantity(qty).build());

        // Descuenta del stock del repuesto el qty consumido

        p.setStock(p.getStock()-qty);
        partRepo.save(p); // guarda el nuevo stock

        // Nota: sería ideal validar que el stock no quede negativo y envolver esto en una transacción (@Transactional).
        // También conviene bloquear o validar cuando la orden esté FINALIZADO/CERRADA (no permitir más consumo).

        return "redirect:/maintenance/" + id; //vuelve al detalle de la orden
    }

    // POST /maintenance/{id}/start -> cambia el estado de la orden a EN_CURSO (inicio de mantenimiento)
    // Nota importante: aquí hay un doble '/maintenance' en la ruta porque la clase YA tiene @RequestMapping("/maintenance").
    // Con el @PostMapping("/maintenance/{id}/start") la URL final sería /maintenance/maintenance/{id}/start.
    @PostMapping("/{id}/start")
    public String start(@PathVariable Long id) {
        var o = orderRepo.findById(id).orElseThrow();
        if (o.getStatus() == MaintStatus.PENDIENTE) {
            o.setStatus(MaintStatus.EN_CURSO);
            orderRepo.save(o);
        }
        return "redirect:/maintenance/" + id;
    }

    // POST /maintenance/{id}/close -> cierra la orden: sube fotos/firma (opcional), marca estado FINALIZADO y fecha de cierre

    @PostMapping("/{id}/close")
    public String close(@PathVariable Long id,
                        @RequestParam(value = "photos", required = false) MultipartFile[] photos,
                        @RequestParam(value = "signatureDataUrl", required = false) String signatureDataUrl,
                        Model model) {

        var order = orderRepo.findById(id).orElseThrow();  // obtiene la orden a cerrar


        // 2.1) Subir FOTOS múltiples a Azure Blob Storage
        if (photos != null) {
            for (MultipartFile f : photos) {
                if (f != null && !f.isEmpty()) {
                    try {
                        String imageUrl = azureBlobService.uploadFile(f);  // sube y obtiene la URL
                        order.setPhotoPaths(appendPath(order.getPhotoPaths(), imageUrl)); // concatena URL a la lista separada por '|' usando el metodo appendPath hecho más abajo
                    } catch (IOException ex) {
                        System.out.println("[WARN] No se pudo subir la foto: " + ex.getMessage());
                    }
                }
            }
        }

        // 2.2) Subir FIRMA a Azure Blob Storage (data URL base64)
        if (signatureDataUrl != null && signatureDataUrl.startsWith("data:image")) {
            try {
                // Extrae la parte Base64 después de la coma

                String b64 = signatureDataUrl.substring(signatureDataUrl.indexOf(",") + 1);
                byte[] png = Base64.getDecoder().decode(b64);

                // Crea un MultipartFile temporal a partir de los bytes para reutilizar el mismo método de subida
                MultipartFile signatureFile = new ByteArrayMultipartFile(png, "signature.png");
                String signatureUrl = azureBlobService.uploadFile(signatureFile); // sube la firma y obtiene URL
                order.setSignaturePath(signatureUrl); // guarda la URL de la firma en la orden
            } catch (IOException e) {
                System.out.println("[WARN] No se pudo subir la firma: " + e.getMessage());
            }
        }

        // 2.3) Cerrar orden: cambia estado y fecha de cierre y guarda

        order.setStatus(MaintStatus.FINALIZADO); // marca como FINALIZADO (ya no debería permitir consumos, cierres, etc.)
        order.setClosedAt(LocalDateTime.now());  // fecha y hora de cierre
        orderRepo.save(order);  // se guarda

        return "redirect:/assets";  // tras cerrar, redirige al listado de activos
    }

    // Helpers
    private String sanitize(String name) {
        if (name == null) return null;
        String s = name.replaceAll("[\\\\/]+", "_");
        s = s.replaceAll("[^a-zA-Z0-9._-]", "_");
        return s;
    }

    // Método utilitario: agrega una ruta (URL) a una cadena existente separada por '|'

    private String appendPath(String existing, String rel) {
        if (existing == null || existing.isBlank()) return rel; // si no hay nada, devuelve el nuevo valor
        // usamos '|' como separador
        return existing + "|" + rel; // concatena preservando el separador estándar del proyecto
    }

    // Esta clase aumenta mucho la complejidad del código.
    // Se deja para en posterior version la complejidad baje y asi mejore la calidad del codigo
    // Clase auxiliar para convertir bytes[] a MultipartFile hay que sacarla y hacerla clase aparte para reducir la complejidad del código.
    static class ByteArrayMultipartFile implements MultipartFile {
        private final byte[] bytes;
        private final String filename;

        public ByteArrayMultipartFile(byte[] bytes, String filename) {
            this.bytes = bytes;
            this.filename = filename;
        }

        @Override
        public String getName() { return filename; } // nombre del parámetro (aquí usamos el mismo que el archivo)

        @Override
        public String getOriginalFilename() { return filename; } // nombre original del archivo

        @Override
        public String getContentType() { return "image/png"; } // tipo de contenido (PNG fijo en este caso)

        @Override
        public boolean isEmpty() { return bytes == null || bytes.length == 0; } // indica si no hay contenido

        @Override
        public long getSize() { return bytes.length; } // tamaño en bytes

        @Override
        public byte[] getBytes() throws IOException { return bytes; } // expone el contenido en bytes

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(bytes); // provee InputStream para APIs que lo requieran
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            // escribe los bytes en el archivo destino (útil si se requiere persistir temporalmente en disco)
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                fos.write(bytes);
            }
        }
    }
}
