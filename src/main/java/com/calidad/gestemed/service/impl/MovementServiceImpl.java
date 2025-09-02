// service/impl/MovementServiceImpl.java
package com.calidad.gestemed.service.impl;

import com.calidad.gestemed.domain.Asset;
import com.calidad.gestemed.domain.AssetMovement;
import com.calidad.gestemed.repo.AssetMovementRepo;
import com.calidad.gestemed.service.MovementService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.*;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MovementServiceImpl implements MovementService {

    private final AssetMovementRepo repo;

    @Value("${movements.retentionDays:365}") // configurable, pero mínimo 90
    private int retentionDays;

    @Override
    public void recordMove(Asset asset, String fromLoc, String toLoc, String reason, String performedBy) {
        if (asset == null) return;
        var mov = AssetMovement.builder()
                .asset(asset)
                .movedAt(LocalDateTime.now())
                .fromLocation(fromLoc)
                .toLocation(toLoc)
                .reason(reason)
                .performedBy(performedBy)
                .build();
        repo.save(mov);
    }

    @Override
    public List<AssetMovement> history(Long assetId, LocalDate from, LocalDate to, String locationLike) {
        var fromDt = (from == null) ? null : from.atStartOfDay();
        // to exclusivo -> sumamos 1 día para incluir el final
        var toDt   = (to   == null) ? null : to.plusDays(1).atStartOfDay();
        var loc = (locationLike == null || locationLike.isBlank()) ? null : locationLike.trim();
        return repo.search(assetId, fromDt, toDt, loc);
    }

    /** Purga automática diaria: NUNCA borra nada con menos de 90 días. */
    @Scheduled(cron = "0 0 3 * * *")
    public void enforceRetention() {
        int days = Math.max(retentionDays, 90);
        var cutoff = LocalDateTime.now().minusDays(days);
        repo.deleteByMovedAtBefore(cutoff);
    }
}
