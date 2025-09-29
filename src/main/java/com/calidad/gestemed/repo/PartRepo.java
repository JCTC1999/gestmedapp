package com.calidad.gestemed.repo;

import com.calidad.gestemed.domain.Part;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface PartRepo extends JpaRepository<Part, Long> {

    // compara columnas: stock < minStock
    @Query("select p from Part p " +
            "where p.stock is not null and p.minStock is not null " +
            "and p.stock < p.minStock")
    List<Part> findLowStock();

}

