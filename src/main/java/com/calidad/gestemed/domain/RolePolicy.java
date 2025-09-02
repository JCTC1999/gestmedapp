package com.calidad.gestemed.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="role_policies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePolicy {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY) Long id;
    @Column(unique=true, nullable=false) String roleName; // p.ej. "WAREHOUSE"
    // permisos simples por módulo
    boolean canAssetsRead, canAssetsWrite;
    boolean canContractsRead, canContractsWrite;
    boolean canInventoryRead, canInventoryWrite;
    boolean canMaintenanceRead, canMaintenanceWrite;
    boolean canReportsRead;
}
