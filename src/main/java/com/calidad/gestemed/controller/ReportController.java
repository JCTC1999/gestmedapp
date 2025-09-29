package com.calidad.gestemed.controller;

import com.calidad.gestemed.domain.Asset;
import com.calidad.gestemed.repo.AssetRepo;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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

    @GetMapping(value="/assets.pdf", produces="application/pdf")
    public ResponseEntity<byte[]> assetsPdf() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Document doc = new Document();
        PdfWriter.getInstance(doc, bos);
        doc.open();
        doc.add(new Paragraph("Inventario de Activos"));
        PdfPTable t = new PdfPTable(2);
        t.addCell("ID Activo");
        t.addCell("Modelo");
        for (Asset a : assetRepo.findAll()) {
            t.addCell(nvl(a.getAssetId()));
            t.addCell(nvl(a.getModel()));
        }
        doc.add(t);
        doc.close();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=activos.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bos.toByteArray());
    }

    @GetMapping(value="/summary.pdf", produces="application/pdf")
    public ResponseEntity<byte[]> summaryPdf() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Document doc = new Document();
        PdfWriter.getInstance(doc, bos);
        doc.open();
        Font font = new Font(Font.HELVETICA, 12);
        doc.add(new Paragraph("Resumen Operativo", font));
        doc.add(new Paragraph("Fecha: " + LocalDate.now(), font));
        doc.add(new Paragraph("Total activos: " + assetRepo.count(), font));

        // Calcular y añadir el monto total de los activos
        List<Asset> assets = assetRepo.findAll();
        BigDecimal totalValue = assets.stream()
                .map(Asset::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        doc.add(new Paragraph("Valor total del inventario: $" + totalValue.toString(), font));

        doc.close();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=resumen.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bos.toByteArray());
    }

    private String nvl(String s) { return s == null ? "" : s; }
}
