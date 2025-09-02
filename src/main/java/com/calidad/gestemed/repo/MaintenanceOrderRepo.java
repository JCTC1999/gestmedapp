package com.calidad.gestemed.repo;

// repo/MaintenanceOrderRepo.java
import com.calidad.gestemed.domain.Asset;
import com.calidad.gestemed.domain.MaintenanceOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaintenanceOrderRepo extends JpaRepository<MaintenanceOrder,Long> {

    List<MaintenanceOrder> findByAsset(Asset asset);


}
