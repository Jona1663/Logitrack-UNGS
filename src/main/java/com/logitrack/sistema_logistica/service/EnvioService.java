package com.logitrack.sistema_logistica.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logitrack.sistema_logistica.dto.EnvioDetalleResponseDTO;
import com.logitrack.sistema_logistica.dto.EnvioRequestDTO;
import com.logitrack.sistema_logistica.dto.HistorialResponseDTO;
import com.logitrack.sistema_logistica.model.Camion;
import com.logitrack.sistema_logistica.model.Chofer_Detalle;
import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.model.Establecimiento;
import com.logitrack.sistema_logistica.model.Historial_Estados;
import com.logitrack.sistema_logistica.model.Usuario;
import com.logitrack.sistema_logistica.model.enums.Estado_Envio;
import com.logitrack.sistema_logistica.repository.CamionRepository;
import com.logitrack.sistema_logistica.repository.Chofer_DetalleRepository;
import com.logitrack.sistema_logistica.repository.EnvioRepository;
import com.logitrack.sistema_logistica.repository.EnvioSpecifications;
import com.logitrack.sistema_logistica.repository.EstablecimientoRepository;
import com.logitrack.sistema_logistica.repository.Historial_EstadosRepository;
import com.logitrack.sistema_logistica.repository.UsuarioRepository;

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

        //#114: Actualización de estado por parte del chofer con validaciones estrictas
        @Transactional
        public Envio actualizarEstadoChofer(String idEnvio, String nuevoEstadoStr, String username) {
                // 1. Buscar el envío
                Envio envio = envioRepository.findById(idEnvio)
                        .orElseThrow(() -> new RuntimeException("Envío no encontrado"));

                // 2. Validación de Identidad: ¿Es su envío asignado? 
                String usernameAsignado = envio.getChofer().getPersona_asociada().getId_usuario().getUsername();
                if (!usernameAsignado.equals(username)) {
                        throw new RuntimeException("Acceso denegado: Este envío no te pertenece");
                }

                // 3. Máquina de Estados: Validar flujo lógico [cite: 49, 111]
                Estado_Envio actual = envio.getEstado_actual();
                Estado_Envio siguiente = Estado_Envio.valueOf(nuevoEstadoStr);

                // NUEVO: Si el estado es el mismo, no hacemos nada y devolvemos el envío tal cual
                if (actual == siguiente) {
                        return envio; 
                }

                if (!esTransicionValida(actual, siguiente)) {
                        throw new RuntimeException("Flujo inválido: No se puede pasar de " + actual + " a " + siguiente);
        }

                // 4. Actualizar (Manteniendo la prioridad intacta) 
                Usuario usuario = usuarioRepository.findByUsername(username).get();
                return actualizarEstadoYPrioridad(idEnvio, nuevoEstadoStr, envio.getPrioridad_ia(), usuario);
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


    /**
     * #114: Validación de Flujo Lógico para Actualización de Estado
     * Método de apoyo: Valida que el chofer siga el flujo lógico 
     * sin saltarse pasos ni retroceder.
     */
    private boolean esTransicionValida(Estado_Envio actual, Estado_Envio siguiente) {
        return switch (actual) {
            case PENDIENTE -> siguiente == Estado_Envio.EN_TRANSITO;
            case EN_TRANSITO -> siguiente == Estado_Envio.EN_PUNTO_DE_RECOLECCION;
            case EN_PUNTO_DE_RECOLECCION -> siguiente == Estado_Envio.EN_REPARTO;
            case EN_REPARTO -> siguiente == Estado_Envio.ENTREGADO;
            default -> false; // El chofer no puede cancelar ni modificar estados finales
        };
    }

    /**
     * #114: Método centralizado para actualizar el estado y la prioridad de un envío,
     * Método de apoyo: Centraliza la actualización del envío y 
     * la creación automática del registro de historial.
     */
    @Transactional
    public Envio actualizarEstadoYPrioridad(String idEnvio, String nuevoEstadoStr, String nuevaPrioridad, 
                                            Usuario usuarioModificador) {
        
        // 1. Buscamos el envío nuevamente para asegurar consistencia
        Envio envio = envioRepository.findById(idEnvio)
                .orElseThrow(() -> new RuntimeException("No se encontró el envío con ID: " + idEnvio));

        Estado_Envio estadoAnterior = envio.getEstado_actual();
        Estado_Envio estadoNuevo = Estado_Envio.valueOf(nuevoEstadoStr);

        // 2. Actualizamos los campos en la entidad
        envio.setEstado_actual(estadoNuevo);
        envio.setPrioridad_ia(nuevaPrioridad); // Aquí el chofer mantiene la que ya tenía

        // 3. Guardamos el envío
        Envio envioGuardado = envioRepository.save(envio);

        // 4. GENERAMOS EL HISTORIAL (Auditoría)
        Historial_Estados historial = Historial_Estados.builder()
                .envio(envioGuardado)
                .usuario(usuarioModificador)
                .estado_anterior(estadoAnterior)
                .estado_nuevo(estadoNuevo)
                .build();

        historialEstadosRepository.save(historial);

        return envioGuardado;
    }

 
        ////////////////////////////////////////////////////////////////////////////////////////////////
       /**
     * #121: Método calcular el ETA (Tiempo Estimado de Llegada) de un envío,
     * Velocidad promedio fija: 65 km/h
        */   
    
    @Transactional
    public void asignarChoferCamion (EnvioRequestDTO dto) {
        Envio envio = envioRepository.findById(dto.getId_envio())
                .orElseThrow(() -> new RuntimeException("Envío no encontrado"));
        Camion camion = camionRepository.findById(dto.getPatente_camion())
                .orElseThrow(() -> new RuntimeException("Camión no encontrado"));
        Chofer_Detalle chofer = choferDetalleRepository.findById(dto.getId_chofer())
            .orElseThrow(() -> new RuntimeException("Chofer no encontrado"));

        
        LocalDateTime fecha_salida = LocalDateTime.now();
        
        envio.setCamion(camion);
        envio.setFecha_estimada_llegada(calcularETA(envio.getDistancia_km(), fecha_salida));
        envio.setFecha_salida(fecha_salida);
        envio.setChofer(chofer);
        envioRepository.save(envio);


    }

    private static final double VELOCIDAD_PROMEDIO_KMH = 65.0;
    private LocalDateTime calcularETA(Double distanciaKm, LocalDateTime fecha_salida) {
        if (distanciaKm == null || distanciaKm <= 0) {
            return null;
        }
        long minutosViaje = Math.round((distanciaKm / VELOCIDAD_PROMEDIO_KMH) * 60);
        return fecha_salida.plusMinutes(minutosViaje);
        }


    /**
    *  #122 — OBTENER DETALLE CON ETA
    *   Usado por el endpoint GET /api/envios/{id}
    */ 

    @Transactional(readOnly = true)
    public EnvioDetalleResponseDTO obtenerDetalleConETA(String idEnvio) {
        Envio envio = envioRepository.findById(idEnvio)
                .orElseThrow(() -> new RuntimeException("No se encontró el envío con ID: " + idEnvio));

        LocalDateTime eta = calcularETA(envio.getDistancia_km(), envio.getFecha_salida());

        return EnvioDetalleResponseDTO.fromEntity(envio, eta);
    }

}
  
