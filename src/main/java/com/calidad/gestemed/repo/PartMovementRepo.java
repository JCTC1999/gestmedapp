package com.calidad.gestemed.repo;

// repo/PartMovementRepo.java
import com.calidad.gestemed.domain.PartMovement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartMovementRepo extends JpaRepository<PartMovement,Long> {}
