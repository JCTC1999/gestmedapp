package com.calidad.gestemed.controller;

import com.calidad.gestemed.domain.Asset;
import com.calidad.gestemed.repo.AssetRepo;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell; // Importante para estilos de celda si se requiere
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.awt.Color; // Para dar color al encabezado de la tabla PDF
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/reports")
public class ReportController {

    private final AssetRepo assetRepo;

    @GetMapping
    public String index() { return "reports/index"; }

    // --- EXCEL DE ACTIVOS (Sin cambios) ---
    @GetMapping(value="/assets.xlsx", produces="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> assetsExcel() throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet s = wb.createSheet("Activos");
            int r = 0;

            // Header
            Row h = s.createRow(r++);
            int c = 0;
            h.createCell(c++).setCellValue("ID Activo");
            h.createCell(c++).setCellValue("Modelo");
            h.createCell(c++).setCellValue("Serial");
            h.createCell(c++).setCellValue("Fabricante");
            h.createCell(c++).setCellValue("Fecha Compra");
            h.createCell(c++).setCellValue("Ubicación");
            h.createCell(c++).setCellValue("Valor");

            // Datos
            for (Asset a : assetRepo.findAll()) {
                Row row = s.createRow(r++);
                int j = 0;
                row.createCell(j++).setCellValue(nvl(a.getAssetId()));
                row.createCell(j++).setCellValue(nvl(a.getModel()));
                row.createCell(j++).setCellValue(nvl(a.getSerialNumber()));
                row.createCell(j++).setCellValue(nvl(a.getManufacturer()));
                row.createCell(j++).setCellValue(a.getPurchaseDate() != null ? a.getPurchaseDate().toString() : "");
                row.createCell(j++).setCellValue(nvl(a.getInitialLocation()));
                row.createCell(j++).setCellValue(a.getValue() != null ? a.getValue().toString() : "");
            }

            for (int i = 0; i < 7; i++) s.autoSizeColumn(i);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=activos.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bos.toByteArray());
        }
    }

    // --- PDF LISTADO SIMPLE (Sin cambios) ---
    @GetMapping(value="/assets.pdf", produces="application/pdf")
    public ResponseEntity<byte[]> assetsPdf() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Document doc = new Document();
        PdfWriter.getInstance(doc, bos);
        doc.open();
        doc.add(new Paragraph("Inventario de Activos"));
        PdfPTable t = new PdfPTable(4);
        t.addCell("ID Activo");
        t.addCell("Modelo");
        t.addCell("serialNumber");
        t.addCell("manufacturer");

        for (Asset a : assetRepo.findAll()) {
            t.addCell(nvl(a.getAssetId()));
            t.addCell(nvl(a.getModel()));
            t.addCell(nvl(a.getSerialNumber()));
            t.addCell(nvl(a.getManufacturer()));

        }
        doc.add(t);
        doc.close();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=activos.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bos.toByteArray());
    }

    // --- PDF RESUMEN (MODIFICADO) ---
    @GetMapping(value="/summary.pdf", produces="application/pdf")
    public ResponseEntity<byte[]> summaryPdf() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Document doc = new Document();
        PdfWriter.getInstance(doc, bos);
        doc.open();

        // Fuentes
        Font fontTitle = new Font(Font.HELVETICA, 18, Font.BOLD);
        Font fontText = new Font(Font.HELVETICA, 12);
        Font fontHeader = new Font(Font.HELVETICA, 12, Font.BOLD, Color.WHITE);

        // 1. Encabezado del Resumen
        doc.add(new Paragraph("Resumen Operativo", fontTitle));
        doc.add(new Paragraph("Fecha de emisión: " + LocalDate.now(), fontText));
        doc.add(new Paragraph(" ")); // Espacio en blanco

        // 2. Cálculos de Totales
        List<Asset> assets = assetRepo.findAll();
        long totalCount = assets.size();
        BigDecimal totalValue = assets.stream()
                .map(Asset::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        doc.add(new Paragraph("Total de activos registrados: " + totalCount, fontText));
        doc.add(new Paragraph("Valor total del inventario: $" + totalValue.toString(), fontText));
        doc.add(new Paragraph(" ")); // Espacio antes de la tabla

        // 3. Tabla Detallada de Activos
        // Creamos una tabla con 4 columnas: ID, Modelo, Fabricante, Valor
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100); // La tabla ocupa todo el ancho
        table.setWidths(new float[]{1.5f, 3f, 2f, 1.5f}); // Anchos relativos de columnas

        // -- Cabecera de la tabla --
        String[] headers = {"ID Activo", "Modelo", "Fabricante", "Valor"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, fontHeader));
            cell.setBackgroundColor(Color.DARK_GRAY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            table.addCell(cell);
        }

        // -- Filas de datos --
        for (Asset a : assets) {
            table.addCell(new Phrase(nvl(a.getAssetId()), fontText));
            table.addCell(new Phrase(nvl(a.getModel()), fontText));
            table.addCell(new Phrase(nvl(a.getManufacturer()), fontText));

            // Formato básico para el valor
            String valorStr = (a.getValue() != null) ? "$" + a.getValue().toString() : "$0.00";
            PdfPCell cellVal = new PdfPCell(new Phrase(valorStr, fontText));
            cellVal.setHorizontalAlignment(Element.ALIGN_RIGHT); // Alinear números a la derecha
            table.addCell(cellVal);
        }

        doc.add(table);

        doc.close();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=resumen.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bos.toByteArray());
    }

    private String nvl(String s) { return s == null ? "" : s; }
}
