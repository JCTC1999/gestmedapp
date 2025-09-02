package com.calidad.gestemed.service;

// service/ContractService.java
import com.calidad.gestemed.domain.Contract;
import java.util.List;

public interface ContractService {
    Contract save(Contract c);
    List<Contract> list();
    void checkAndNotifyExpiring(); // alerta por vencer/vencidos
}
