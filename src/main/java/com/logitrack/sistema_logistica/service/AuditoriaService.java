package com.logitrack.sistema_logistica.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logitrack.sistema_logistica.dto.HistorialResponseDTO;
import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.model.HistorialEstados;
import com.logitrack.sistema_logistica.model.Usuario;
import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;
import com.logitrack.sistema_logistica.model.enums.TipoEvento;
import com.logitrack.sistema_logistica.repository.HistorialEstadosRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final HistorialEstadosRepository historialEstadosRepository;

    /**
     * Centraliza la creación y guardado de cualquier evento de auditoría.
     */
    @Transactional
    public void registrarEvento(Envio envio, Usuario usuario, TipoEvento tipoEvento, 
                                EstadoEnvio estadoAnterior, EstadoEnvio estadoNuevo) {
        
        HistorialEstados historial = HistorialEstados.builder()
                .envio(envio)
                .usuario(usuario)
                .tipoEvento(tipoEvento)
                .estadoAnterior(estadoAnterior)
                .estadoNuevo(estadoNuevo)
                .build();

        historialEstadosRepository.save(historial);
    }

    /**
     * Mapea el historial de un envío específico.
     */
    @Transactional(readOnly = true)
    public List<HistorialResponseDTO> obtenerHistorialPorEnvio(String idEnvio) {
        return historialEstadosRepository.buscarHistorialPorEnvio(idEnvio)
                .stream()
                .map(HistorialResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }
}