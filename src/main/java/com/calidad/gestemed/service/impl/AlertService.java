package com.calidad.gestemed.service.impl;

import com.calidad.gestemed.domain.Contract;
import com.calidad.gestemed.repo.ContractRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertService {
    private final ContractRepo contractRepo;
    private final JavaMailSender mailSender; // si no configuras, inyecta opcionalmente

    public List<Contract> contractsExpiringSoon() {
        LocalDate today = LocalDate.now();
        // Trae todos y filtra con su propio alertDays
        return contractRepo.findAll().stream()
                .filter(c -> c.getEndDate()!=null && !c.getEndDate().isBefore(today))
                .filter(c -> {
                    int alert = c.getAlertDays()==null?0:c.getAlertDays();
                    return !c.getEndDate().isAfter(today.plusDays(alert));
                })
                .toList();
    }

    public void sendExpiryEmailsIfConfigured(List<Contract> list) {
        try {
            for (Contract c : list) {
                // arma correo sencillo
                var msg = new org.springframework.mail.SimpleMailMessage();
                msg.setTo("demo@local"); // cambia o toma del cliente si lo guardas
                msg.setSubject("Contrato por vencer: " + c.getCode());
                msg.setText("El contrato " + c.getCode() + " vence el " + c.getEndDate());
                mailSender.send(msg);
            }
        } catch (Exception e) {
            // si no hay SMTP configurado, no fallamos: solo log
            System.out.println("[ALERT] Emails no enviados (sin SMTP): " + e.getMessage());
        }
    }
}

