// service/GeofenceService.java
package com.calidad.gestemed.service.impl;

import com.calidad.gestemed.domain.*;
import com.calidad.gestemed.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;


import java.time.LocalDateTime;
import java.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service @RequiredArgsConstructor
public class GeofenceService {
    private final GeofenceRepo geofenceRepo;
    private final GeofenceStateRepo stateRepo;
    private final GeofenceEventRepo eventRepo;
    private final NotificationRepo notificationRepo;
    private final JavaMailSender mailSender;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${app.mail.from:${spring.mail.username}}")
    private String from;

    private final String alertsEmailTo = System.getProperty("app.alerts.to", "jctorrescalderon@gmail.com");

    /* Se llama cuando llega una nueva posición de un activo. */


    //Este es el método principal que se activa con cada nueva posición de GPS
//Este es el método principal que se activa con cada nueva posición de GPS
    @Transactional
    public void processPosition(Asset asset, double lat, double lng) {
        List<Geofence> fences = geofenceRepo.findApplicable(asset);
        for (Geofence g : fences) {
            boolean insideNow = pointInPolygon(lat, lng, parsePolygon(g.getPolygonJson()));

            // Busca el estado existente o crea uno nuevo en memoria si no lo encuentra.
            GeofenceState st = stateRepo.findByAssetAndGeofence(asset, g)
                    .orElse(GeofenceState.builder()
                            .asset(asset).geofence(g).inside(insideNow)
                            .updatedAt(LocalDateTime.now())
                            .build());

            // --- LÓGICA CORREGIDA ---
            // 'firstTime' es verdadero si el estado no tiene ID (nunca se ha guardado en la BD).
            boolean firstTime = (st.getId() == null);
            // 'changed' es verdadero si NO es la primera vez Y el estado ha cambiado.
            boolean changed = !firstTime && (st.isInside() != insideNow);

            // Se debe generar un evento si es la primera vez o si el estado ha cambiado.
            if (firstTime || changed) {
                GeofenceEvent.Type type = insideNow ? GeofenceEvent.Type.ENTER : GeofenceEvent.Type.EXIT;

                // ¿Debemos alertar?
                boolean shouldAlert = (insideNow && g.isAlertOnEnter()) || (!insideNow && g.isAlertOnExit());

                // Guarda el evento en la base de datos.
                eventRepo.save(GeofenceEvent.builder()
                        .asset(asset).geofence(g).type(type)
                        .lat(lat).lng(lng)
                        .occurredAt(LocalDateTime.now())
                        .build());

                if (shouldAlert) {
                    // Determina la acción (ENTRÓ o SALIÓ)
                    String action = insideNow ? "ENTRÓ" : "SALIÓ";
                    // Determina la preposición ('a' para entrar, 'de' para salir)
                    String preposition = insideNow ? "a" : "de";

                    String msg = "Alerta geocerca: activo " + asset.getAssetId() + " " +
                            action + " " + preposition + " '" + g.getName() + "'.";

                    // Panel (Notification)
                    notificationRepo.save(Notification.builder()
                            .createdAt(LocalDateTime.now())
                            .message(msg)
                            .readFlag(false)
                            .build());
                    // Email
                    try {
                        SimpleMailMessage mail = new SimpleMailMessage();
                        mail.setFrom(from);
                        mail.setTo(alertsEmailTo);
                        mail.setSubject("Alerta geocerca - " + asset.getAssetId());
                        mail.setText(msg + "\nUbicación: lat=" + lat + " lng=" + lng + "\nFecha: " + LocalDateTime.now());
                        mailSender.send(mail);
                    } catch (Exception ignore) {  }
                }
            }

            // Actualiza el estado actual del activo dentro de la geocerca.
            st.setInside(insideNow);
            st.setUpdatedAt(LocalDateTime.now());
            // Guarda el estado en la BD (sea nuevo o una actualización).
            stateRepo.save(st);
        }
    }

    /* ---------- Utilidades geometría ---------- */

    private List<double[]> parsePolygon(String polygonJson) {
        // Espera: [ {lat:..., lng:...}, ... ]
        try {
            List<Map<String, Object>> list = mapper.readValue(polygonJson, new TypeReference<>(){});
            List<double[]> poly = new ArrayList<>();
            for (var p : list) {
                double la = ((Number)p.get("lat")).doubleValue();
                double ln = ((Number)p.get("lng")).doubleValue();
                poly.add(new double[]{la, ln});
            }
            return poly;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /* Ray casting: lat/lng en la convención usual. Polígono simple, no auto-cerrado. */
    private boolean pointInPolygon(double lat, double lng, List<double[]> poly) {
        boolean inside = false;
        for (int i = 0, j = poly.size() - 1; i < poly.size(); j = i++) {
            double yi = poly.get(i)[0], xi = poly.get(i)[1];
            double yj = poly.get(j)[0], xj = poly.get(j)[1];
            boolean intersect = ((yi > lat) != (yj > lat)) &&
                    (lng < (xj - xi) * (lat - yi) / (yj - yi + 0.0) + xi);
            if (intersect) inside = !inside;
        }
        return inside;
    }
}
