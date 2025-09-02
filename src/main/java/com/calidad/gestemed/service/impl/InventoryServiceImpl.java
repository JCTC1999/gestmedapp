package com.calidad.gestemed.service.impl;

// service/impl/InventoryServiceImpl.java

import com.calidad.gestemed.domain.Notification;
import com.calidad.gestemed.domain.Part;
import com.calidad.gestemed.domain.PartMovement;
import com.calidad.gestemed.repo.NotificationRepo;
import com.calidad.gestemed.repo.PartMovementRepo;
import com.calidad.gestemed.repo.PartRepo;
import com.calidad.gestemed.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service @RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {
    private final PartRepo partRepo;
    private final PartMovementRepo movementRepo;
    private final NotificationRepo notificationRepo;

    @Override
    public void adjustStock(Long partId, int delta, String note) {
        Part p = partRepo.findById(partId).orElseThrow();
        p.setStock((p.getStock()==null?0:p.getStock()) + delta);
        partRepo.save(p);
        movementRepo.save(PartMovement.builder()
                .part(p).delta(delta).note(note).createdAt(LocalDateTime.now()).build());
    }

    // Diaria 08:05
    @Scheduled(cron="0 5 8 * * *")
    @Override public void checkLowStockAndNotify() {
        partRepo.findAll().forEach(p -> {
            if (p.getMinStock()!=null && p.getStock()!=null && p.getStock() <= p.getMinStock()) {
                notificationRepo.save(Notification.builder()
                        .message("Stock bajo en refacción: "+p.getName()+" (actual="+p.getStock()+", min="+p.getMinStock()+")")
                        .createdAt(LocalDateTime.now()).build());
            }
        });
    }
}
