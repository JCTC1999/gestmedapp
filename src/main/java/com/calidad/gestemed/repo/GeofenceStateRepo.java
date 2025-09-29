// repo/GeofenceStateRepo.java
package com.calidad.gestemed.repo;

import com.calidad.gestemed.domain.Asset;
import com.calidad.gestemed.domain.Geofence;
import com.calidad.gestemed.domain.GeofenceState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GeofenceStateRepo extends JpaRepository<GeofenceState, Long> {
    Optional<GeofenceState> findByAssetAndGeofence(Asset asset, Geofence geofence);
}
