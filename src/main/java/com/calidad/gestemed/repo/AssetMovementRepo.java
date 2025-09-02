// repo/AssetMovementRepo.java
package com.calidad.gestemed.repo;

import com.calidad.gestemed.domain.AssetMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface AssetMovementRepo extends JpaRepository<AssetMovement, Long> {

    @Query("""
       select m from AssetMovement m
       where m.asset.id = :assetId
         and (:fromDate is null or m.movedAt >= :fromDate)
         and (:toDate   is null or m.movedAt <  :toDate)
         and (:loc is null or lower(m.fromLocation) like lower(concat('%',:loc,'%'))
                         or lower(m.toLocation)   like lower(concat('%',:loc,'%')))
       order by m.movedAt desc
    """)
    List<AssetMovement> search(Long assetId, LocalDateTime fromDate, LocalDateTime toDate, String loc);

    long deleteByMovedAtBefore(LocalDateTime cutoff);
}

