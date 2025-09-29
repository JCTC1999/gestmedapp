// src/main/java/com/calidad/gestemed/dto/ActaForm.java
package com.calidad.gestemed.dto;

import com.calidad.gestemed.domain.Acta;

public class ActaForm {
    private Long assetId;
    private Acta.Tipo type;       // ENTREGA o DEVOLUCION
    private Long contractId;      // opcional
    private String notes;         // opcional

    // getters/setters
    public Long getAssetId() { return assetId; }
    public void setAssetId(Long assetId) { this.assetId = assetId; }
    public Acta.Tipo getType() { return type; }
    public void setType(Acta.Tipo type) { this.type = type; }
    public Long getContractId() { return contractId; }
    public void setContractId(Long contractId) { this.contractId = contractId; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
