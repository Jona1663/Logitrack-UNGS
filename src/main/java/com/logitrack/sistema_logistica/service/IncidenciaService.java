package com.logitrack.sistema_logistica.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logitrack.sistema_logistica.dto.AlertaListadoDTO;
import com.logitrack.sistema_logistica.dto.ChoferAlertaDTO;
import com.logitrack.sistema_logistica.dto.IncidenciaDTO;
import com.logitrack.sistema_logistica.dto.ResolverIncidenciaDTO;
import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.model.Incidencia;
import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;
import com.logitrack.sistema_logistica.model.enums.EstadoIncidencia;
import com.logitrack.sistema_logistica.repository.EnvioRepository;
import com.logitrack.sistema_logistica.repository.IncidenciaRepository;

@Service
public class IncidenciaService {

    @Autowired
    private IncidenciaRepository incidenciaRepository;

    @Autowired
    private EnvioRepository envioRepository;

    @Autowired
    private TrackingGeospatialService trackingService; // Inyectamos el servicio de ubicaciones

    @Autowired
    private NotificationService notificationService; // Inyectamos el de tu compañero

    @Transactional
    public void reportarIncidencia(String idEnvio, IncidenciaDTO dto) {
        // 1. Buscamos el viaje
        Envio envio = envioRepository.findById(idEnvio)
                .orElseThrow(() -> new RuntimeException("Envío no encontrado"));

        // 2. Regla de negocio: Bloqueo de alertas sin viaje activo (Criterio 3 del Chofer)
        if (envio.getEstadoActual() != EstadoEnvio.EN_TRANSITO && envio.getEstadoActual() != EstadoEnvio.EN_REPARTO) {
            throw new IllegalStateException("Solo se pueden reportar incidencias sobre viajes en curso.");
        }

        // 3.  Calculamos dónde está el camión ahora mismo
        String ubicacionFormateada = "Ubicación no disponible";
        try {
            Map<String, Object> ubicacion = trackingService.calcularUbicacionInterpolada(envio);
            ubicacionFormateada = "Lat: " + ubicacion.get("latitudActual") + ", Lon: " + ubicacion.get("longitudActual");
        } catch (Exception e) {
            // Si falla el tracking (ej. no hay ruta), guardamos el error pero no bloqueamos la alerta
            ubicacionFormateada = "Error calculando ubicación: " + e.getMessage();
        }

        String usuarioActual = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();

        // 4. Creamos la entidad
        Incidencia nuevaIncidencia = Incidencia.builder()
                .envio(envio)
                .tipoIncidencia(dto.getTipoIncidencia())
                .descripcion(dto.getDescripcion())
                .estado(EstadoIncidencia.PENDIENTE)
                .fechaReporte(LocalDateTime.now())
                .lugarIncidencia(ubicacionFormateada) 
                .creadoPor(usuarioActual)
                .build();

        incidenciaRepository.save(nuevaIncidencia);

        // 5. Enviamos la notificación al supervisor
        String asunto = "NUEVA ALERTA: " + dto.getTipoIncidencia().name() + " en viaje " + idEnvio;
        String mensaje = "El chofer ha reportado un problema.\nUbicación: " + ubicacionFormateada + "\nDetalle: " + dto.getDescripcion();
        
        notificationService.enviarNotificacion("supervisor@logitrack.com", asunto, mensaje);
    }

    // --- MÉTODOS DEL SUPERVISOR ---

    @Transactional(readOnly = true)
    public List<AlertaListadoDTO> listarAlertas() {
        List<Incidencia> incidencias = incidenciaRepository.findAllByOrderByFechaReporteDesc();

        // Mapeamos de Entidad a DTO para el frontend
        return incidencias.stream().map(inc -> {
            
            // Extraemos datos del chofer (Manejo seguro de nulos por si el envío no tiene chofer asignado)
            ChoferAlertaDTO choferDto = null;
            if (inc.getEnvio().getChofer() != null && inc.getEnvio().getChofer().getPersonaAsociada() != null) {
                choferDto = ChoferAlertaDTO.builder()
                        .id(inc.getEnvio().getChofer().getIdChofer())
                        .nombreCompleto(inc.getEnvio().getChofer().getPersonaAsociada().getNombre() + " " + inc.getEnvio().getChofer().getPersonaAsociada().getApellido())
                        .telefono(inc.getEnvio().getChofer().getPersonaAsociada().getTelefono())
                        .build();
            }

            return AlertaListadoDTO.builder()
                    .id(inc.getIdIncidencia())
                    .idEnvio(inc.getEnvio().getIdEnvio())
                    .chofer(choferDto)
                    .tipoIncidencia(inc.getTipoIncidencia().name())
                    .descripcion(inc.getDescripcion())
                    .estado(inc.getEstado().name())
                    .fechaReporte(inc.getFechaReporte())
                    .fechaResolucion(inc.getFechaResolucion())
                    .lugarIncidencia(inc.getLugarIncidencia())
                    .build();
        }).toList();
    }

    @Transactional
    public void resolverIncidencia(Integer idIncidencia, ResolverIncidenciaDTO dto) {
        Incidencia incidencia = incidenciaRepository.findById(idIncidencia)
                .orElseThrow(() -> new RuntimeException("Incidencia no encontrada"));

        incidencia.setEstado(EstadoIncidencia.RESUELTA);
        incidencia.setFechaResolucion(LocalDateTime.now());
        
        if (dto != null && dto.getNotasSupervisor() != null) {
            incidencia.setNotasSupervisor(dto.getNotasSupervisor());
        }

        incidenciaRepository.save(incidencia);
    }
}