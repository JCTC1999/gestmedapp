package com.calidad.gestemed.domain;
// domain/Contract.java

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.Set;

@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Contract {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    private String code;
    private String clientName;
    private LocalDate startDate;
    private LocalDate endDate;

    @Column(length=2000)
    private String terms;

    // 30/60/90 días (MVP: por contrato)
    private Integer alertDays;

    @ManyToMany
    @JoinTable(name="contract_assets",
            joinColumns=@JoinColumn(name="contract_id"),
            inverseJoinColumns=@JoinColumn(name="asset_id"))

    /*  Se usa Set porque:
        Un contrato no debería tener el mismo asset repetido.
        No les importa el orden de los assets, solo que estén.
     */
    private Set<Asset> assets;

    @Enumerated(EnumType.STRING)
    private ContractStatus status; // VIGENTE, POR_VENCER, VENCIDO



    /*
    Anotación @Transient
        Es una anotación de JPA/Hibernate.

        Indica que este método no se mapea a la base de datos.

        Es decir, el status no se guarda en la tabla, se calcula en tiempo real.
     */
    @Transient
    public String getStatus() {
        java.time.LocalDate today = java.time.LocalDate.now();
        if (endDate == null) return "VIGENTE";
        if (endDate.isBefore(today)) return "VENCIDO";
        int alert = (alertDays == null ? 0 : alertDays);
        java.time.LocalDate warnFrom = today.plusDays(alert);
        return (!endDate.isAfter(warnFrom)) ? "POR_VENCER" : "VIGENTE";
    }

}


