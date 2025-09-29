package com.calidad.gestemed.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// domain/Asset.java
@Entity
@Table(name="assets", uniqueConstraints=@UniqueConstraint(columnNames={"assetId"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Asset {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Column(nullable=false)
    private String assetId;

    @NotBlank private String model;
    @NotBlank private String serialNumber;
    @NotBlank private String manufacturer;

    @NotNull private LocalDate purchaseDate;
    @NotBlank private String initialLocation;

    @Column(name = "asset_value", nullable = false)
    @NotNull private BigDecimal value;

    private String rfidOrQrTag;

    @Column(length=4000)
    private String photoPaths;

    private LocalDateTime createdAt;
    private String createdBy;

    // ---- NUEVO: posición GPS "actual"
    @Column(name = "last_latitude")
    private Double lastLatitude;

    @Column(name = "last_longitude")
    private Double lastLongitude;

    @Column(name = "last_gps_at")
    private LocalDateTime lastGpsAt;
}


