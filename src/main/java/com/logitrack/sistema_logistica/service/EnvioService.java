package com.logitrack.sistema_logistica.service;

import com.logitrack.sistema_logistica.dto.EnvioRequestDTO;
import com.logitrack.sistema_logistica.dto.HistorialResponseDTO;
import com.logitrack.sistema_logistica.model.*;
import com.logitrack.sistema_logistica.model.enums.Estado_Envio;
import com.logitrack.sistema_logistica.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EnvioService {

    @Autowired private Historial_EstadosRepository historialRepository;
    @Autowired private EnvioRepository envioRepository;
    @Autowired private EstablecimientoRepository establecimientoRepository;
    @Autowired private Chofer_DetalleRepository choferDetalleRepository;
    @Autowired private CamionRepository camionRepository;
    @Autowired private Historial_EstadosRepository historialEstadosRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    @Transactional // Si algo falla, no se guarda ni el envío ni el historial
    public Envio crearNuevoEnvio(EnvioRequestDTO dto) {
        
        // 1. Buscar todas las relaciones en la Base de Datos
        Establecimiento origen = establecimientoRepository.findById(dto.getId_origen())
                .orElseThrow(() -> new RuntimeException("Establecimiento de origen no encontrado"));
                
        Establecimiento destino = establecimientoRepository.findById(dto.getId_destino())
                .orElseThrow(() -> new RuntimeException("Establecimiento de destino no encontrado"));
                
        Chofer_Detalle chofer = choferDetalleRepository.findById(dto.getId_chofer())
                .orElseThrow(() -> new RuntimeException("Chofer no encontrado"));
                
        Camion camion = camionRepository.findById(dto.getPatente_camion())
                .orElseThrow(() -> new RuntimeException("Camión no encontrado"));
                
        Usuario usuarioCreador = usuarioRepository.findById(dto.getId_usuario_creador())
                .orElseThrow(() -> new RuntimeException("Usuario creador no encontrado"));

        // 2. Construir el objeto Envio
        Envio nuevoEnvio = Envio.builder()
                .id_envio(dto.getId_envio())
                .cpe(dto.getCpe())
                .origen(origen)
                .destino(destino)
                .chofer(chofer)
                .camion(camion)
                .tipo_grano(dto.getTipo_grano())
                .prioridad_ia(dto.getPrioridad_ia())
                .kg_origen(dto.getKg_origen())
                .estado_actual(Estado_Envio.PENDIENTE) // Todo envío nace como PENDIENTE
                .build();

        // 3. Guardar el Envío (Acá se autogenera el id "LT-XXXXXX" y la fecha)
        nuevoEnvio = envioRepository.save(nuevoEnvio);

        // 4. Crear y guardar el Historial inicial
        Historial_Estados historial = Historial_Estados.builder()
                .envio(nuevoEnvio)
                .usuario(usuarioCreador)
                .estado_nuevo(Estado_Envio.PENDIENTE)
                // estado_anterior queda en null porque es el primer estado
                .build();
                
        historialEstadosRepository.save(historial);

        // 5. Retornar el envío ya creado
        return nuevoEnvio;
    }

    //que pasa si el envío existe o si no se encuentra.
    public Envio buscarPorId(String id_envio) {
        return envioRepository.buscarPorId(id_envio)
            .orElseThrow(() -> new RuntimeException("No se encontró el envío con el id_envio: " + id_envio));
    }

    public Page<Envio> buscarEnviosConFiltros(Estado_Envio estado, LocalDateTime fechaInicio, LocalDateTime fechaFin, String termino, Pageable pageable) {
        Specification<Envio> spec = Specification.where(EnvioSpecifications.tieneEstado(estado))
                .and(EnvioSpecifications.fechaCreacionEntre(fechaInicio, fechaFin))
                .and(EnvioSpecifications.contieneTermino(termino));
        return envioRepository.findAll(spec, pageable);
    }

    //#113
    // Lógica de obtención
    // Conecta la identidad del usuario con la base de datos.
    public List<Envio> obtenerEnviosPorChofer(String username) {
        return envioRepository.findByChoferUsername(username);
    }       


    /**
     * Obtiene el historial de eventos de un envío por su identificador.
     * Primero valida que el envío exista y luego devuelve los registros de historial
     * transformados a DTO para exponer solo los campos necesarios.
     */
    @Transactional(readOnly = true)
    public List<HistorialResponseDTO> obtenerHistorialPorEnvio(String idEnvio) {
        // Validar existencia del envío antes de consultar el historial
        if (!envioRepository.existsById(idEnvio)) {
            throw new RuntimeException("No se encontró el envío con id_envio: " + idEnvio);
        }

        // Buscar los registros de historial ordenados por fecha descendente
        return historialRepository.buscarHistorialPorEnvio(idEnvio)
                .stream()
                .map(HistorialResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional // Garantiza que si falla el historial, no se guarde el envío a medias
        public Envio actualizarEstadoYPrioridad(String idEnvio, String nuevoEstadoStr, String nuevaPrioridad,
                        Usuario usuarioModificador) {

                // 1. Buscar el envío existente por su ID principal (LT-XXXXXX)
                Envio envio = envioRepository.findById(idEnvio)
                                .orElseThrow(() -> new RuntimeException("No se encontró el envío con ID: " + idEnvio));

                // 2. Capturar el estado actual antes de modificarlo para el historial
                Estado_Envio estadoAnterior = envio.getEstado_actual();

                // Convertir el String que viene del DTO/Frontend al Enum de Java
                Estado_Envio estadoNuevo = Estado_Envio.valueOf(nuevoEstadoStr);

                // 3. Verificar qué datos cambiaron realmente
                boolean estadoCambio = !estadoAnterior.equals(estadoNuevo);
                boolean prioridadCambio = (nuevaPrioridad != null && !nuevaPrioridad.equals(envio.getPrioridad_ia()));

                // Si no hubo cambios reales, simplemente devolvemos el envío tal cual
                if (!estadoCambio && !prioridadCambio) {
                        return envio;
                }

                // 4. Actualizar los valores en el objeto Envio
                if (estadoCambio) {
                        envio.setEstado_actual(estadoNuevo);
                }
                if (prioridadCambio) {
                        envio.setPrioridad_ia(nuevaPrioridad);
                }

                // 5. Guardar el envío actualizado
                Envio envioGuardado = envioRepository.save(envio);

                // 6. Generar el registro de Auditoría SOLO si el estado logístico cambió
                if (estadoCambio) {
                        Historial_Estados historial = Historial_Estados.builder()
                                        .envio(envioGuardado)
                                        .usuario(usuarioModificador)
                                        .estado_anterior(estadoAnterior)
                                        .estado_nuevo(estadoNuevo)
                                        // La fecha_hora se genera sola por el @PrePersist en tu modelo
                                        .build();

                        historialEstadosRepository.save(historial);
                }

                return envioGuardado;
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////
}
  
