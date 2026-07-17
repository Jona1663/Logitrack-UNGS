package com.logitrack.sistema_logistica.service;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.logitrack.sistema_logistica.dto.ClienteRequestDTO;
import com.logitrack.sistema_logistica.model.EmpresaCliente;
import com.logitrack.sistema_logistica.model.Establecimiento;
import com.logitrack.sistema_logistica.model.enums.TipoEmpresa;
import com.logitrack.sistema_logistica.repository.EmpresaClienteRepository;
import com.logitrack.sistema_logistica.repository.EstablecimientoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final EmpresaClienteRepository empresaClienteRepository;
    private final EstablecimientoRepository establecimientoRepository;

    @Transactional
    public EmpresaCliente crearCliente(ClienteRequestDTO dto) {

        if (empresaClienteRepository.existsById(dto.cuit())) {
            throw new IllegalArgumentException("Ya existe un cliente con este CUIT");
        }

        EmpresaCliente cliente = EmpresaCliente.builder()
                .cuit(dto.cuit())
                .razonSocial(dto.razonSocial().trim())
                .tipoEmpresa(TipoEmpresa.valueOf(dto.tipoEmpresa()))
                .email(dto.email())
                .rucaNro(dto.rucaNro())
                .vtoRuca(dto.vtoRuca() != null ? LocalDate.parse(dto.vtoRuca()) : null)
                .build();

        EmpresaCliente clienteGuardado = empresaClienteRepository.save(cliente);

        if (dto.sede() != null && dto.sede().nombreLugar() != null) {
            Establecimiento sede = Establecimiento.builder()
                    .empresa(clienteGuardado)
                    .nombreLugar(dto.sede().nombreLugar())
                    .direccion(dto.sede().direccion() != null ? dto.sede().direccion() : "")
                    .latitud(dto.sede().latitud())
                    .longitud(dto.sede().longitud())
                    .build();
            establecimientoRepository.save(sede);
        }

        return clienteGuardado;
    }

    public List<EmpresaCliente> listarClientes() {
        return empresaClienteRepository.findAll();
    }
}