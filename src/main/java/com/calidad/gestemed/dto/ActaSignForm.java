// src/main/java/com/calidad/gestemed/dto/ActaSignForm.java
package com.calidad.gestemed.dto;

public class ActaSignForm {
    private String signatureDataUrl; // data:image/png;base64,...
    private Double lat;              // opcional (manual)
    private Double lng;              // opcional (manual)

    // getters/setters
    public String getSignatureDataUrl() { return signatureDataUrl; }
    public void setSignatureDataUrl(String signatureDataUrl) { this.signatureDataUrl = signatureDataUrl; }
    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }
    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }
}
