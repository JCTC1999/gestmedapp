// service/MovementService.java
package com.calidad.gestemed.service;

import com.calidad.gestemed.domain.Asset;
import com.calidad.gestemed.domain.AssetMovement;

import java.time.LocalDate;
import java.util.List;

public interface MovementService {
    void recordMove(Asset asset, String fromLoc, String toLoc, String reason, String performedBy);
    List<AssetMovement> history(Long assetId, LocalDate from, LocalDate to, String locationLike);
}

