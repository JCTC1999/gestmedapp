package com.calidad.gestemed.repo;

import com.calidad.gestemed.domain.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssetRepo extends JpaRepository<Asset,Long> {
    Optional<Asset> findByAssetId(String assetId);
    boolean existsByAssetId(String assetId);



    @Query(value = """
        select distinct a.*
        from assets a
        left join contract_assets ca on ca.asset_id = a.id
        left join contract c on c.id = ca.contract_id
        where a.last_latitude is not null
          and a.last_longitude is not null
          and (:contractId is null or c.id = :contractId)
          and (:clientLike is null or lower(c.client_name) like :clientLike)
          and (:status is null or c.status = :status)
        """, nativeQuery = true)
    List<Asset> findForGps(@Param("clientLike") String clientLike,
                           @Param("contractId") Long contractId,
                           @Param("status") String status);

}