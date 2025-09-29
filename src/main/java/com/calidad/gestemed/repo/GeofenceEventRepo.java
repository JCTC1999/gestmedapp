// repo/GeofenceEventRepo.java
package com.calidad.gestemed.repo;

import com.calidad.gestemed.domain.GeofenceEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeofenceEventRepo extends JpaRepository<GeofenceEvent, Long> { }
