package com.calidad.gestemed.controller;

// ... (existing imports)
import com.calidad.gestemed.domain.Asset;
import com.calidad.gestemed.repo.AssetRepo;
import com.opencsv.CSVReader;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Este controlador maneja la carga de archivos CSV y XLSX para la importación de datos.
// Incluye lógica estándar para detectar formatos, leer el contenido,
// y mapear las filas a objetos de la base de datos, evitando duplicados.
// Es una plantilla común para procesos de importación.

@Controller @RequiredArgsConstructor
@RequestMapping("/import")
public class ImportController {
    private final AssetRepo assetRepo;

    @GetMapping public String form() { return "import/form"; }

    @PostMapping
    public String upload(@RequestParam("file") MultipartFile file, Model model) {
        try {
            if (file.isEmpty()) {
                model.addAttribute("error", "Selecciona un archivo CSV o XLSX.");
                return "import/form";
            }

            String name = (file.getOriginalFilename() == null ? "" : file.getOriginalFilename()).toLowerCase();

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUser = (authentication != null) ? authentication.getName() : "system";
            LocalDateTime now = LocalDateTime.now();

            if (name.endsWith(".csv")) {
                String content = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
                content = content.replace("\uFEFF", "");
                char sep = detectSeparator(content);

                com.opencsv.CSVParser parser = new com.opencsv.CSVParserBuilder().withSeparator(sep).build();
                try (com.opencsv.CSVReader r = new com.opencsv.CSVReaderBuilder(new java.io.StringReader(content)).withCSVParser(parser).build()) {
                    java.util.List<com.calidad.gestemed.domain.Asset> imported = new java.util.ArrayList<>();
                    String[] row;
                    boolean header = true;
                    int line = 0;

                    while ((row = r.readNext()) != null) {
                        line++;
                        if (header) { header = false; continue; }
                        if (row.length == 0) continue;
                        if (row.length == 1 && (row[0] == null || row[0].isBlank())) continue;

                        if (row.length < 8) {
                            model.addAttribute("error", "Formato CSV inválido en línea " + line + ": se esperaban 8 columnas y llegaron " + row.length);
                            return "import/form";
                        }
                        for (int i = 0; i < row.length; i++) {
                            row[i] = (row[i] == null ? "" : row[i].trim());
                        }

                        com.calidad.gestemed.domain.Asset a = mapRow(
                                row[0], row[1], row[2], row[3], row[4], row[5], row[6], row[7]
                        );

                        // Establecer valores por defecto para GPS
                        a.setLastLatitude(9.853517);
                        a.setLastLongitude(-83.908713);
                        a.setLastGpsAt(now);

                        a.setCreatedBy(currentUser);
                        a.setCreatedAt(now);

                        if (!assetRepo.existsByAssetId(a.getAssetId())) {
                            imported.add(assetRepo.save(a));
                        }
                    }

                    model.addAttribute("count", imported.size());
                    return "import/success";
                }

            } else if (name.endsWith(".xlsx")) {
                java.util.List<com.calidad.gestemed.domain.Asset> imported = new java.util.ArrayList<>();
                try (org.apache.poi.ss.usermodel.Workbook wb = org.apache.poi.ss.usermodel.WorkbookFactory.create(file.getInputStream())) {
                    org.apache.poi.ss.usermodel.Sheet s = wb.getSheetAt(0);
                    boolean header = true;
                    for (org.apache.poi.ss.usermodel.Row row : s) {
                        if (header) { header = false; continue; }
                        if (row == null) continue;
                        String assetId = getCellString(row.getCell(0));
                        if (assetId.isBlank()) continue;

                        com.calidad.gestemed.domain.Asset a = mapRow(
                                assetId,
                                getCellString(row.getCell(1)),
                                getCellString(row.getCell(2)),
                                getCellString(row.getCell(3)),
                                getCellDate(row.getCell(4)),
                                getCellString(row.getCell(5)),
                                getCellString(row.getCell(6)),
                                getCellString(row.getCell(7))
                        );

                        // Establecer valores por defecto para GPS
                        a.setLastLatitude(9.853517);
                        a.setLastLongitude(-83.908713);
                        a.setLastGpsAt(now);

                        a.setCreatedBy(currentUser);
                        a.setCreatedAt(now);

                        if (!assetRepo.existsByAssetId(a.getAssetId())) {
                            imported.add(assetRepo.save(a));
                        }
                    }
                }
                model.addAttribute("count", imported.size());
                return "import/success";
            } else {
                model.addAttribute("error", "Formato no soportado. Sube un .csv o .xlsx");
                return "import/form";
            }

        } catch (Exception e) {
            model.addAttribute("error", "No se pudo importar: " + e.getMessage());
            return "import/form";
        }
    }


    private Asset mapRow(String assetId, String model, String serial, String maker,
                         String purchase, String location, String value, String photoPaths) {
        return Asset.builder()
                .assetId(assetId)
                .model(model)
                .serialNumber(serial)
                .manufacturer(maker)
                .purchaseDate(LocalDate.parse(purchase))
                .initialLocation(location)
                .value(new BigDecimal(value))
                .photoPaths(photoPaths)
                .build();
    }

    private char detectSeparator(String content) {
        String firstLine = content.lines().findFirst().orElse("");
        int commas = firstLine.length() - firstLine.replace(",", "").length();
        int semis  = firstLine.length() - firstLine.replace(";", "").length();
        return (semis > commas) ? ';' : ',';
    }

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        String s;
        CellType type = cell.getCellType();
        switch (type) {
            case STRING:
                s = cell.getStringCellValue();
                break;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    s = cell.getLocalDateTimeCellValue().toLocalDate().toString();
                } else {
                    double v = cell.getNumericCellValue();
                    long lv = (long) v;
                    s = (v == lv) ? Long.toString(lv) : Double.toString(v);
                }
                break;
            case BOOLEAN:
                s = Boolean.toString(cell.getBooleanCellValue());
                break;
            case FORMULA:
                s = cell.getCellFormula();
                break;
            default:
                s = "";
        }
        return (s == null) ? "" : s.trim();
    }

    private String getCellDate(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate().toString();
        }
        return getCellString(cell);
    }
}
