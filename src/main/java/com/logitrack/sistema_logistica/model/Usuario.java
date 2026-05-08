package com.logitrack.sistema_logistica.model;

import com.logitrack.sistema_logistica.model.enums.Rol_Usuario;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "Usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_usuario;

    @Column(unique = true, nullable = false, length = 100)
    private String username;// es un mail

    @Column(nullable = false, length = 255)
    private String password_hash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Rol_Usuario rol;

    @Builder.Default
    @Column(columnDefinition = "boolean default true")
    private Boolean activo = true;
}