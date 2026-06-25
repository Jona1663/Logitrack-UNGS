package com.logitrack.sistema_logistica.model;

import com.logitrack.sistema_logistica.model.enums.RolUsuario;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Data;

@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idUsuario;

    @Column(unique = true, nullable = false, length = 100)
    private String username;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RolUsuario rol;

    @Builder.Default
    @Column(columnDefinition = "boolean default true")
    private Boolean activo = true;

    @Builder.Default
    @Column(name = "intentos_fallidos", columnDefinition = "integer default 0")
    private Integer intentosFallidos = 0;

    @Builder.Default
    @Column(columnDefinition = "boolean default false")
    private Boolean bloqueado = false;

    @Column(name = "codigo_desbloqueo", length = 6)
    private String codigoDesbloqueo;

    @Column(name = "vencimiento_codigo")
    private LocalDateTime vencimientoCodigo;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;
}