package com.calidad.gestemed.service.impl;

import com.calidad.gestemed.domain.Notification;
import com.calidad.gestemed.domain.Part;
import com.calidad.gestemed.domain.PartMovement;
import com.calidad.gestemed.repo.NotificationRepo;
import com.calidad.gestemed.repo.PartMovementRepo;
import com.calidad.gestemed.repo.PartRepo;
import com.calidad.gestemed.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service @RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final PartRepo partRepo;
    private final PartMovementRepo movementRepo;
    private final NotificationRepo notificationRepo;

    // Inyectamos el enviador de correos
    private final JavaMailSender mailSender;

    // Leemos el remitente desde la configuración
    @Value("${app.mail.from:${spring.mail.username}}")
    private String fromEmail;

    // Definimos el destinatario (puedes hacerlo configurable en application.properties si prefieres)
    private final String alertRecipient = "jctorrescalderon@gmail.com";

    @Override
    public void adjustStock(Long partId, int delta, String note) {
        Part p = partRepo.findById(partId).orElseThrow();
        p.setStock((p.getStock()==null?0:p.getStock()) + delta);
        partRepo.save(p);
        movementRepo.save(PartMovement.builder()
                .part(p).delta(delta).note(note).createdAt(LocalDateTime.now()).build());
    }

    // Diaria 08:05
    @Scheduled(cron="0 0 20 * * *")
    @Override public void checkLowStockAndNotify() {
        partRepo.findAll().forEach(p -> {
            // Verificamos si el stock es menor o igual al mínimo
            if (p.getMinStock() != null && p.getStock() != null && p.getStock() <= p.getMinStock()) {

                String alertMsg = "Stock bajo en refacción: " + p.getName() +
                        " (Actual=" + p.getStock() + ", Mínimo=" + p.getMinStock() + ")";

                // 1. Guardar notificación en Base de Datos (Para el panel web)
                notificationRepo.save(Notification.builder()
                        .message(alertMsg)
                        .createdAt(LocalDateTime.now()).build());

                // 2. Enviar Correo Electrónico
                try {
                    SimpleMailMessage email = new SimpleMailMessage();
                    email.setFrom(fromEmail);
                    email.setTo(alertRecipient);
                    email.setSubject("ALERTA DE INVENTARIO: " + p.getName());
                    email.setText("Estimado usuario,\n\n" +
                            "El sistema ha detectado niveles bajos de inventario.\n\n" +
                            "Refacción: " + p.getName() + "\n" +
                            "Stock Actual: " + p.getStock() + "\n" +
                            "Stock Mínimo Requerido: " + p.getMinStock() + "\n\n" +
                            "Por favor gestione el reabastecimiento lo antes posible.\n" +
                            "Fecha: " + LocalDateTime.now());

                    mailSender.send(email);
                    System.out.println("Correo enviado para: " + p.getName());

                } catch (Exception e) {
                    // Logueamos el error pero NO detenemos el proceso para las otras refacciones
                    System.err.println("Error enviando correo de stock bajo para " + p.getName() + ": " + e.getMessage());
                }
            }
        });
    }
}
