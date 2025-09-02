package com.calidad.gestemed.dto;

import java.time.LocalDateTime;

// /dto/GpsDTO.java
public record GpsDTO(
        Long id,
        String assetId,
        String model,
        Double lat,
        Double lng,
        LocalDateTime lastGpsAt
) {}

