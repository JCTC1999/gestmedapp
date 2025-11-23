package com.calidad.gestemed.controller;

import com.calidad.gestemed.domain.Asset;
import com.calidad.gestemed.repo.AssetRepo;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/import")
public class ImportController {
    private final AssetRepo assetRepo;

    @GetMapping
    public String form() {
        return "import/form";
    }

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

            int duplicates = 0; // <--- NUEVO CONTADOR DE DUPLICADOS
            List<Asset> imported = new ArrayList<>();

            // ==========================================
            // LÓGICA PARA CSV
            // ==========================================
            if (name.endsWith(".csv")) {
                String content = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
                content = content.replace("\uFEFF", "");
                char sep = detectSeparator(content);

                com.opencsv.CSVParser parser = new com.opencsv.CSVParserBuilder().withSeparator(sep).build();
                try (com.opencsv.CSVReader r = new com.opencsv.CSVReaderBuilder(new java.io.StringReader(content)).withCSVParser(parser).build()) {

                    String[] row;
                    boolean header = true;
                    int line = 0;

                    while ((row = r.readNext()) != null) {
                        line++;
                        if (header) { header = false; continue; }
                        if (row.length == 0 || (row.length == 1 && (row[0] == null || row[0].isBlank()))) continue;

                        if (row.length < 8) {
                            throw new IllegalArgumentException("Error en fila CSV " + line + ": Se esperaban 8 columnas, llegaron " + row.length + ".");
                        }

                        for (int i = 0; i < row.length; i++) row[i] = (row[i] == null ? "" : row[i].trim());

                        try {
                            if (row[0].isEmpty()) throw new IllegalArgumentException("El ID del Activo es obligatorio.");

                            // Validar duplicados ANTES de crear el objeto para ahorrar memoria
                            if (assetRepo.existsByAssetId(row[0])) {
                                duplicates++; // <--- CONTAMOS DUPLICADO
                                continue;     // <--- SALTAMOS A LA SIGUIENTE FILA
                            }

                            Asset a = mapRow(row[0], row[1], row[2], row[3], row[4], row[5], row[6], row[7]);

                            // Defaults
                            a.setLastLatitude(9.853517);
                            a.setLastLongitude(-83.908713);
                            a.setLastGpsAt(now);
                            a.setCreatedBy(currentUser);
                            a.setCreatedAt(now);

                            imported.add(assetRepo.save(a));

                        } catch (DateTimeParseException e) {
                            throw new IllegalArgumentException("Error en fila CSV " + line + ": Fecha inválida ('" + row[4] + "').");
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Error en fila CSV " + line + ": Valor numérico inválido ('" + row[6] + "').");
                        } catch (Exception e) {
                            throw new IllegalArgumentException("Error en fila CSV " + line + ": " + e.getMessage());
                        }
                    }
                }

                // ==========================================
                // LÓGICA PARA EXCEL (XLSX)
                // ==========================================
            } else if (name.endsWith(".xlsx")) {
                try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
                    Sheet s = wb.getSheetAt(0);
                    boolean header = true;

                    for (Row row : s) {
                        int rowNum = row.getRowNum() + 1;
                        if (header) { header = false; continue; }
                        if (row == null) continue;

                        String assetId = getCellString(row.getCell(0));
                        if (assetId.isBlank()) continue;

                        // Validación básica de estructura Excel
                        if (row.getLastCellNum() < 8) {
                            // Opcional: lanzar error o advertir. Aquí lanzamos error para ser consistentes con CSV.
                            // throw new IllegalArgumentException("Error en Excel fila " + rowNum + ": Faltan columnas.");
                        }

                        try {
                            // Validar duplicados
                            if (assetRepo.existsByAssetId(assetId)) {
                                duplicates++; // <--- CONTAMOS DUPLICADO
                                continue;
                            }

                            Asset a = mapRow(
                                    assetId,
                                    getCellString(row.getCell(1)),
                                    getCellString(row.getCell(2)),
                                    getCellString(row.getCell(3)),
                                    getCellDate(row.getCell(4)),
                                    getCellString(row.getCell(5)),
                                    getCellString(row.getCell(6)),
                                    getCellString(row.getCell(7))
                            );

                            a.setLastLatitude(9.853517);
                            a.setLastLongitude(-83.908713);
                            a.setLastGpsAt(now);
                            a.setCreatedBy(currentUser);
                            a.setCreatedAt(now);

                            imported.add(assetRepo.save(a));

                        } catch (Exception e) {
                            throw new IllegalArgumentException("Error en Excel fila " + rowNum + ": " + e.getMessage());
                        }
                    }
                }
            } else {
                model.addAttribute("error", "Formato no soportado.");
                return "import/form";
            }

            // --- ÉXITO: ENVIAMOS LOS CONTADORES A LA VISTA ---
            model.addAttribute("count", imported.size());
            model.addAttribute("duplicates", duplicates); // <--- ENVIAMOS DUPLICADOS

            return "import/success";

        } catch (Exception e) {
            model.addAttribute("error", "Falló la importación: " + e.getMessage());
            return "import/form";
        }
    }

    // ... (Mantén los métodos mapRow, detectSeparator, getCellString, getCellDate igual que antes)
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
            case STRING: s = cell.getStringCellValue(); break;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    s = cell.getLocalDateTimeCellValue().toLocalDate().toString();
                } else {
                    double v = cell.getNumericCellValue();
                    long lv = (long) v;
                    s = (v == lv) ? Long.toString(lv) : Double.toString(v);
                }
                break;
            case BOOLEAN: s = Boolean.toString(cell.getBooleanCellValue()); break;
            case FORMULA: s = cell.getCellFormula(); break;
            default: s = "";
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
