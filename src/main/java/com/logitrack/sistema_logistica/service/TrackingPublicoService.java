package com.logitrack.sistema_logistica.service;

import com.logitrack.sistema_logistica.dto.TrackingPublicoRequestDTO;
import com.logitrack.sistema_logistica.dto.TrackingPublicoResponseDTO;
import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.repository.EnvioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TrackingPublicoService {
    @Autowired
    private EnvioRepository envioRepository;

    public TrackingPublicoResponseDTO obtenerInfoPublica(TrackingPublicoRequestDTO request) {
        // 1. Buscar el envío en la base de datos
        Envio envio = envioRepository.findById(request.getTrackingId())
                .orElseThrow(() -> new RuntimeException("No encontrado"));

        // 2. Acceder al CUIT siguiendo la estructura del modelo
        String cuitOrigen = envio.getOrigen().getEmpresa().getCuit();
        String cuitDestino = envio.getDestino().getEmpresa().getCuit();

        // 3. Validar coincidencia
        if (!request.getCuit().equals(cuitOrigen) && !request.getCuit().equals(cuitDestino)) {
            throw new RuntimeException("No encontrado");
        }

        Integer porcentaje = calcularPorcentaje(envio);

        // 4. Mapear al DTO
        return TrackingPublicoResponseDTO.builder()
                .trackingId(envio.getIdEnvio())
                .estadoActual(envio.getEstadoActual().toString())
                .origenNombre(envio.getOrigen().getNombreLugar())
                .destinoNombre(envio.getDestino().getNombreLugar())
                .fechaCreacion(envio.getFechaCreacion() != null ? envio.getFechaCreacion().toString() : null)
                .fechaSalida(envio.getFechaSalida() != null ? envio.getFechaSalida().toString() : null)
                .eta(envio.getFechaEstimadaLlegada() != null ? envio.getFechaEstimadaLlegada().toString() : null)
                .porcentajeCompletado(porcentaje)
                // Usamos las coordenadas del destino o una fija si el camión no tiene datos GPS
                .ubicacionActual(envio.getDestino() != null ? 
                    new com.logitrack.sistema_logistica.dto.UbicacionDTO(
                        envio.getDestino().getLatitud(), 
                        envio.getDestino().getLongitud()
                    ) : null)
                .build();
    }

    private Integer calcularPorcentaje(Envio envio) {
        if (envio.getEstadoActual() == null) return 0;
        
        // Usamos los estados exactos definidos en tu Enum EstadoEnvio
        return switch (envio.getEstadoActual()) {
            case PENDIENTE -> 0;
            case EN_PUNTO_DE_RECOLECCION -> 20;
            case EN_TRANSITO -> 50;
            case EN_REPARTO -> 80;
            case ENTREGADO -> 100;
            case CANCELADO -> 0;
            default -> 0;
        };
    }
}
