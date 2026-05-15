package com.logitrack.sistema_logistica.controller;

import com.logitrack.sistema_logistica.dto.MetadatosDTO;
import com.logitrack.sistema_logistica.model.*;
import com.logitrack.sistema_logistica.model.enums.*;
import com.logitrack.sistema_logistica.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/catalogos")
public class CatalogoController {

    @Autowired private EmpresaClienteRepository empresaRepository;
    @Autowired private EstablecimientoRepository establecimientoRepository;
    @Autowired private ChoferDetalleRepository choferRepository;
    @Autowired private CamionRepository camionRepository;

    // 1. Empresas Clientes
    @GetMapping("/empresas")
    public List<EmpresaCliente> getEmpresas() {
        return empresaRepository.findAll();
    }

    // 2. Establecimientos (Orígenes/Destinos)
    @GetMapping("/establecimientos")
    public List<Establecimiento> getEstablecimientos() {
        return establecimientoRepository.findAll();
    }

    // 3. Choferes
    @GetMapping("/choferes")
    public List<ChoferDetalle> getChoferes() {
        return choferRepository.findAll();
    }

    // 4. Camiones
    @GetMapping("/camiones")
    public List<Camion> getCamiones() {
        return camionRepository.findAll();
    }

    // 5. ENUMS (Metadatos dinámicos)
    // Esto es muy valorado en la industria porque si agregás un grano en Java, el Front se actualiza solo.
    @GetMapping("/metadatos")
    public MetadatosDTO getMetadatos() {
        return MetadatosDTO.builder()
                .categorias(Arrays.stream(Categoria.values()).map(Enum::name).collect(Collectors.toList()))
                .estadosEnvio(Arrays.stream(EstadoEnvio.values()).map(Enum::name).collect(Collectors.toList()))
                .rolesUsuario(Arrays.stream(RolUsuario.values()).map(Enum::name).collect(Collectors.toList()))
                .tiposEmpresa(Arrays.stream(TipoEmpresa.values()).map(Enum::name).collect(Collectors.toList()))
                .tiposGrano(Arrays.stream(TipoGrano.values()).map(Enum::name).collect(Collectors.toList()))
                .build();
    }
}