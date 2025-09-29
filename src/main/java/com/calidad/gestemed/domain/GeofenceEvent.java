// domain/GeofenceEvent.java
package com.calidad.gestemed.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
/*
    Clase especifica para alertas de geocerca

    Asocia directamente un evento con un Asset y una GeoFence en particular


 */
@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name="geofence_events")
public class GeofenceEvent {
    public enum Type { ENTER, EXIT }

    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional=false) private Asset asset;
    @ManyToOne(optional=false) private Geofence geofence;

    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private Type type;

    private double lat;
    private double lng;
    private LocalDateTime occurredAt;
    private String note; // opcional
}
