// repo/AssetMovementRepo.java
package com.calidad.gestemed.repo;

import com.calidad.gestemed.domain.AssetMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AssetMovementRepo extends JpaRepository<AssetMovement, Long> {
    @Query(value = """
          select *
          from asset_movements m
          where m.asset_id = :assetId
            and m.moved_at >= :fromDate
            and m.moved_at <  :toDate
            and (
                  :pattern is null
                  or m.from_location ILIKE :pattern
                  or m.to_location   ILIKE :pattern
                )
          order by m.moved_at desc
        """, nativeQuery = true)
    List<AssetMovement> searchNative(
            @Param("assetId") Long assetId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate")   LocalDateTime toDate,
            @Param("pattern")  String pattern
    );

    long deleteByMovedAtBefore(LocalDateTime cutoff);
}

