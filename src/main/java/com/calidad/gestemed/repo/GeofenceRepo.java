// repo/GeofenceRepo.java
package com.calidad.gestemed.repo;

import com.calidad.gestemed.domain.Asset;
import com.calidad.gestemed.domain.Geofence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GeofenceRepo extends JpaRepository<Geofence, Long> {

    /* Geocercas activas que aplican al activo (o a todos si assets vacío). */
    @Query("""
      select g from Geofence g
      where g.active = true
        and ( size(g.assets) = 0 or :asset member of g.assets )
    """)
    List<Geofence> findApplicable(@Param("asset") Asset asset);
}
