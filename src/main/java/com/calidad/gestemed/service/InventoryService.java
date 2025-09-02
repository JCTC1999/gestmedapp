package com.calidad.gestemed.service;

// service/InventoryService.java
public interface InventoryService {
    void adjustStock(Long partId, int delta, String note);
    void checkLowStockAndNotify();
}
