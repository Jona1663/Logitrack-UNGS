package com.logitrack.sistema_logistica.controller;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logitrack.sistema_logistica.dto.MetadatosDTO;
import com.logitrack.sistema_logistica.model.Camion;
import com.logitrack.sistema_logistica.model.ChoferDetalle;
import com.logitrack.sistema_logistica.model.EmpresaCliente;
import com.logitrack.sistema_logistica.model.Establecimiento;
import com.logitrack.sistema_logistica.model.enums.Categoria;
import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;
import com.logitrack.sistema_logistica.model.enums.RolUsuario;
import com.logitrack.sistema_logistica.model.enums.TipoEmpresa;
import com.logitrack.sistema_logistica.model.enums.TipoGrano;
import com.logitrack.sistema_logistica.repository.CamionRepository;
import com.logitrack.sistema_logistica.repository.ChoferDetalleRepository;
import com.logitrack.sistema_logistica.repository.EmpresaClienteRepository;
import com.logitrack.sistema_logistica.repository.EstablecimientoRepository;

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
    // Choferes disponibles (sin viaje activo)
    @GetMapping("/choferes/disponibles")
    public List<ChoferDetalle> getChoferesDisponibles() {
        List<EstadoEnvio> estadosActivos = Arrays.asList(
            EstadoEnvio.EN_TRANSITO,
            EstadoEnvio.EN_PUNTO_DE_RECOLECCION,
            EstadoEnvio.EN_REPARTO
        );
        return choferRepository.findChoferesDisponibles(estadosActivos);
}

    // Camiones disponibles (sin viaje activo)
    @GetMapping("/camiones/disponibles")
    public List<Camion> getCamionesDisponibles() {
        List<EstadoEnvio> estadosActivos = Arrays.asList(
            EstadoEnvio.EN_TRANSITO,
            EstadoEnvio.EN_PUNTO_DE_RECOLECCION,
            EstadoEnvio.EN_REPARTO
        );
        return camionRepository.findCamionesDisponibles(estadosActivos);
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