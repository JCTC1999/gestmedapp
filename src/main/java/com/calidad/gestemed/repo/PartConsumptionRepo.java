package com.calidad.gestemed.repo;

// repo/PartConsumptionRepo.java
import com.calidad.gestemed.domain.PartConsumption;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PartConsumptionRepo extends JpaRepository<PartConsumption,Long> {}
