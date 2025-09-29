// domain/Geofence.java
package com.calidad.gestemed.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name="geofences")
public class Geofence {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Column(nullable=false)
    private String name;

    /* JSON con [{ "lat":14.6, "lng":-90.5 }, ...] del polígono (en sentido horario/cerrado en front). */
    @Lob @Column(nullable=false)
    private String polygonJson;

    /* Activación */
    @Builder.Default
    private boolean active = true;

    /* ¿Alertar al entrar/salir? (MVP: ambas true) */
    @Builder.Default
    private boolean alertOnEnter = true;
    @Builder.Default
    private boolean alertOnExit = true;

    /* Si la lista está vacía aplica a TODOS los activos */
    @ManyToMany
    @JoinTable(name="geofence_assets",
            joinColumns=@JoinColumn(name="geofence_id"),
            inverseJoinColumns=@JoinColumn(name="asset_id"))
    @Builder.Default
    private Set<Asset> assets = new HashSet<>();
}
