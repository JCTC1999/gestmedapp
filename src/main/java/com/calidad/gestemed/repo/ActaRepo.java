// src/main/java/com/calidad/gestemed/repo/ActaRepo.java
package com.calidad.gestemed.repo;

import com.calidad.gestemed.domain.Acta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ActaRepo extends JpaRepository<Acta, Long> {

}

