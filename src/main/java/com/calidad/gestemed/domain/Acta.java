package com.calidad.gestemed.domain;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// domain/Acta.java
@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Acta {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public enum Tipo { ENTREGA, DEVOLUCION }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Tipo tipo;

    @ManyToOne(optional = false)
    private Asset asset;                // Activo al que aplica

    @ManyToOne(optional = true)
    private Contract contract;          // Opcional: si la entrega/devolución es por contrato

    private LocalDateTime createdAt;
    private String createdBy;

    // Firma y ubicación capturadas en el momento de firmar:
    private String signaturePath;       // URL en Azure
    private Double signatureLat;
    private Double signatureLng;
    private LocalDateTime signedAt;

    @Column(length=2000)
    private String notes;
}
