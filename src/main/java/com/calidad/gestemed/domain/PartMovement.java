package com.calidad.gestemed.domain;

// domain/PartMovement.java

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PartMovement {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional=false) private Part part;
    private Integer delta; // >0 entrada, <0 salida
    private String note;
    private LocalDateTime createdAt;
}
