// domain/GeofenceState.java
package com.calidad.gestemed.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;



//Estado de un activo respecto a una geocerca


/*
La función principal de GeoFenceState es servir como una cache de estado para cada par de activo y geocerca.
Almacena el último estado conocido (inside: true/false) de un activo con respecto a una geocerca.
 */

@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name="geofence_state",
        uniqueConstraints=@UniqueConstraint(columnNames={"asset_id","geofence_id"}))
public class GeofenceState {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional=false) private Asset asset;
    @ManyToOne(optional=false) private Geofence geofence;

    private boolean inside;                 // último estado conocido
    private LocalDateTime updatedAt;        // última evaluación
}
