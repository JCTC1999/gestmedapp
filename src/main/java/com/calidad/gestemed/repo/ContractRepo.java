package com.calidad.gestemed.repo;

// repo/ContractRepo.java
import com.calidad.gestemed.domain.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface ContractRepo extends JpaRepository<Contract,Long> {
    List<Contract> findByEndDateBetween(LocalDate from, LocalDate to);
    List<Contract> findByEndDateBefore(LocalDate date);

    @Query("select c from Contract c where c.endDate between :from and :to")
    List<Contract> findExpiringBetween(LocalDate from, LocalDate to);


    @Query("select distinct c.status from Contract c where c.status is not null")
    List<String> distinctStatuses();

}

