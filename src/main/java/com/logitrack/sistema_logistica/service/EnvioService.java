package com.logitrack.sistema_logistica.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.logitrack.sistema_logistica.dto.AsignarTransporteDTO;
import com.logitrack.sistema_logistica.dto.EnvioDetalleResponseDTO;
import com.logitrack.sistema_logistica.dto.EnvioOperativoDTO;
import com.logitrack.sistema_logistica.dto.EnvioRequestDTO;
import com.logitrack.sistema_logistica.dto.HistorialResponseDTO;
import com.logitrack.sistema_logistica.dto.ReasignacionViajeRequestDTO;
import com.logitrack.sistema_logistica.events.EnvioCambioEstadoEvent;
import com.logitrack.sistema_logistica.events.EnvioCambioEstadoEventNotificaciones;
import com.logitrack.sistema_logistica.events.EnvioNuevoEvent;
import com.logitrack.sistema_logistica.model.Camion;
import com.logitrack.sistema_logistica.model.ChoferDetalle;
import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.model.Establecimiento;
import com.logitrack.sistema_logistica.model.EvaluacionPsicomotora;
import com.logitrack.sistema_logistica.model.Usuario;
import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;
import com.logitrack.sistema_logistica.model.enums.EstadoEvaluacionEnum;
import com.logitrack.sistema_logistica.model.enums.TipoEvento;
import com.logitrack.sistema_logistica.model.enums.TipoGrano;
import com.logitrack.sistema_logistica.repository.CamionRepository;
import com.logitrack.sistema_logistica.repository.ChoferDetalleRepository;
import com.logitrack.sistema_logistica.repository.EnvioRepository;
import com.logitrack.sistema_logistica.repository.EnvioSpecifications;
import com.logitrack.sistema_logistica.repository.EstablecimientoRepository;
import com.logitrack.sistema_logistica.repository.EvaluacionPsicomotoraRepository;
import com.logitrack.sistema_logistica.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;

@Service
public class EnvioService {
        @Autowired
        private EnvioRepository envioRepository;
        @Autowired
        private EstablecimientoRepository establecimientoRepository;
        @Autowired
        private ChoferDetalleRepository choferDetalleRepository;
        @Autowired
        private CamionRepository camionRepository;
        @Autowired
        private UsuarioRepository usuarioRepository;
        @Autowired
        private ValidacionExternaService validacionExternaService;
        @Autowired
        private TrackingGeospatialService trackingService;
        @Autowired
        private AuditoriaService auditoriaService;
        @Autowired
        private ApplicationEventPublisher eventPublisher;
        @Autowired
        private EvaluacionPsicomotoraRepository evaluacionRepository;

        @Transactional // Si algo falla, no se guarda ni el envío ni el historial
        public Envio crearNuevoEnvio(EnvioRequestDTO dto) {
                java.time.LocalDate hoy = java.time.LocalDate.now();
                // validacion CPE
                String nroAutorizacionArca = validacionExternaService.getNroAutorizacionArca(dto.getCpe());

                // Buscar todas las relaciones en la Base de Datos
                Establecimiento origen = establecimientoRepository.findById(dto.getIdOrigen())
                                .orElseThrow(() -> new RuntimeException("Establecimiento de origen no encontrado"));
                validacionExternaService.verificarRucaEmpresa(hoy, origen);

                Establecimiento destino = establecimientoRepository.findById(dto.getIdDestino())
                                .orElseThrow(() -> new RuntimeException("Establecimiento de destino no encontrado"));
                validacionExternaService.verificarRucaEmpresa(hoy, destino);

                ChoferDetalle chofer = (dto.getIdChofer() != null)
                                ? choferDetalleRepository.findById(dto.getIdChofer()).orElse(null)
                                : null;

                Camion camion = (dto.getPatenteCamion() != null && !dto.getPatenteCamion().isBlank())
                                ? camionRepository.findById(dto.getPatenteCamion()).orElse(null)
                                : null;

                Usuario usuarioCreador = usuarioRepository.findById(dto.getIdUsuarioCreador())
                                .orElseThrow(() -> new RuntimeException("Usuario creador no encontrado"));

                // Construir el objeto Envio
                Envio nuevoEnvio = Envio.builder()
                                .idEnvio(dto.getIdEnvio())
                                .cpe(dto.getCpe())
                                .autorizacionARCA(nroAutorizacionArca)
                                .origen(origen)
                                .destino(destino)
                                .chofer(chofer)
                                .camion(camion)
                                .tipoGrano(dto.getTipoGrano())
                                .prioridadIa(dto.getPrioridadIa())
                                .kgOrigen(dto.getKgOrigen())
                                .estadoActual(EstadoEnvio.PENDIENTE) // Todo envío nace como PENDIENTE
                                .build();

                // Guardar el Envío (Acá se autogenera el id "LT-XXXXXX" y la fecha)
                nuevoEnvio = envioRepository.save(nuevoEnvio);

                // Crear y guardar el Historial inicial
                auditoriaService.registrarEvento(
                                nuevoEnvio,
                                usuarioCreador,
                                TipoEvento.CREACION,
                                null,
                                EstadoEnvio.PENDIENTE);

                // Publicar evento
                eventPublisher.publishEvent(new EnvioNuevoEvent(this, nuevoEnvio));

                // Retornar el envío ya creado
                return nuevoEnvio;
        }

        // que pasa si el envío existe o si no se encuentra.
        public Envio buscarPorId(String idEnvio) {
                return envioRepository.buscarPorId(idEnvio)
                                .orElseThrow(() -> new RuntimeException(
                                                "No se encontró el envío con el idEnvio: " + idEnvio));
        }

        public Page<Envio> buscarEnviosConFiltros(EstadoEnvio estado, LocalDateTime fechaInicio,
                        LocalDateTime fechaFin, String termino, String tipoGrano, Boolean asignado, Pageable pageable) {
                Specification<Envio> spec = Specification.where(EnvioSpecifications.tieneEstado(estado))
                                .and(EnvioSpecifications.fechaCreacionEntre(fechaInicio, fechaFin))
                                .and(EnvioSpecifications.contieneTermino(termino))
                                .and(EnvioSpecifications.esDeTipoGrano(tipoGrano))
                                .and(EnvioSpecifications.tieneAsignacion(asignado));
                return envioRepository.findAll(spec, pageable);
        }

        // Conecta la identidad del usuario con la base de datos.
        public List<Envio> obtenerEnviosPorChofer(String username) {
                return envioRepository.findByChoferUsername(username);
        }

        // Actualización de estado por parte del chofer con validaciones estrictas
        @Transactional
        public Envio actualizarEstadoChofer(String idEnvio, String nuevoEstadoStr, String username) {
                // Buscar el envío
                Envio envio = envioRepository.findById(idEnvio)
                                .orElseThrow(() -> new RuntimeException("Envío no encontrado"));

                // Validación de Identidad: ¿Es su envío asignado?
                String usernameAsignado = envio.getChofer().getPersonaAsociada().getIdUsuario().getUsername();
                if (!usernameAsignado.equals(username)) {
                        throw new RuntimeException("Acceso denegado: Este envío no te pertenece");
                }

                // Máquina de Estados: Validar flujo lógico [cite: 49, 111]
                EstadoEnvio actual = envio.getEstadoActual();
                EstadoEnvio siguiente = EstadoEnvio.valueOf(nuevoEstadoStr);

                // Si el estado es el mismo, no hacemos nada y devolvemos el envío tal cual
                if (actual == siguiente) {
                        return envio;
                }

                if (!esTransicionValida(actual, siguiente)) {
                        throw new RuntimeException(
                                        "Flujo inválido: No se puede pasar de " + actual + " a " + siguiente);
                }

                // Actualizar (Manteniendo la prioridad intacta)
                Usuario usuario = usuarioRepository.findByUsername(username).get();
                return actualizarEstadoYPrioridad(idEnvio, nuevoEstadoStr, envio.getPrioridadIa(), usuario,
                                TipoEvento.CAMBIO_ESTADO);
        }

        @Transactional(readOnly = true)
        public List<HistorialResponseDTO> obtenerHistorialPorEnvio(String idEnvio) {
                // Validar existencia del envío antes de consultar el historial
                if (!envioRepository.existsById(idEnvio)) {
                        throw new RuntimeException("No se encontró el envío con idEnvio: " + idEnvio);
                }

                // Buscar los registros de historial ordenados por fecha descendente
                return auditoriaService.obtenerHistorialPorEnvio(idEnvio);
        }

        private boolean esTransicionValida(EstadoEnvio actual, EstadoEnvio siguiente) {
                return switch (actual) {
                        case PENDIENTE -> siguiente == EstadoEnvio.EN_TRANSITO;
                        case EN_TRANSITO -> siguiente == EstadoEnvio.EN_PUNTO_DE_RECOLECCION;
                        case EN_PUNTO_DE_RECOLECCION -> siguiente == EstadoEnvio.EN_REPARTO;
                        case EN_REPARTO -> siguiente == EstadoEnvio.ENTREGADO;
                        default -> false; // El chofer no puede cancelar ni modificar estados finales
                };
        }

        @Transactional
        public Envio actualizarEstadoYPrioridad(String idEnvio, String nuevoEstadoStr, String nuevaPrioridad,
                        Usuario usuarioModificador, TipoEvento eventoRealizado) {

                // Buscamos el envío nuevamente para asegurar consistencia
                Envio envio = envioRepository.findById(idEnvio)
                                .orElseThrow(() -> new RuntimeException("No se encontró el envío con ID: " + idEnvio));

                EstadoEnvio estadoAnterior = envio.getEstadoActual();
                EstadoEnvio estadoNuevo = EstadoEnvio.valueOf(nuevoEstadoStr);

                if (estadoNuevo == EstadoEnvio.EN_TRANSITO) {
                        boolean tieneRechazo = evaluacionRepository.existsByEnvioIdEnvioAndEstadoBloqueo(
                                        idEnvio, EstadoEvaluacionEnum.RECHAZADO);

                        if (tieneRechazo) {
                                throw new RuntimeException(
                                                "Acción bloqueada: El chofer tiene una evaluación de fatiga rechazada y ACTIVA.");
                        }
                }

                // Actualizamos los campos en la entidad
                envio.setEstadoActual(estadoNuevo);
                envio.setPrioridadIa(nuevaPrioridad); // Aquí el chofer mantiene la que ya tenía

                // Si el estado cambia a En transito o Enreparto , pedimos la ruta
                if (estadoNuevo == EstadoEnvio.EN_TRANSITO || estadoNuevo == EstadoEnvio.EN_PUNTO_DE_RECOLECCION ||
                                estadoNuevo == EstadoEnvio.EN_REPARTO) {
                        try {
                                trackingService.generarYGuardarRuta(envio);
                        } catch (Exception e) {

                                System.err.println("Advertencia de Ruteo: " + e.getMessage());
                        }
                }

                // Liberamos a los choferes y camiónes si el envío termina o se cancela
                if (estadoNuevo == EstadoEnvio.ENTREGADO || estadoNuevo == EstadoEnvio.CANCELADO) {
                        if (estadoNuevo == EstadoEnvio.ENTREGADO) {
                                envio.setFechaLlegada(LocalDateTime.now());
                                if (envio.getKgDestino() == null) {
                                        envio.setKgDestino(envio.getKgOrigen());
                                }
                        }
                        if (envio.getChofer() != null) {
                                envio.getChofer().setDisponible(true);
                                choferDetalleRepository.save(envio.getChofer());

                        }
                        if (envio.getCamion() != null) {
                                envio.getCamion().setDisponible(true);
                                camionRepository.save(envio.getCamion());
                        }
                }

                if (estadoNuevo == EstadoEnvio.EN_REPARTO) {
                        try {
                                // Esto fuerza al servicio de Tracking a recalcular la ruta
                                // desde la posición actual del camión hasta el destino final.
                                trackingService.generarYGuardarRuta(envio);
                        } catch (Exception e) {
                                System.err.println("Error al regenerar ruta en EN_REPARTO: " + e.getMessage());
                        }
                }

                // Guardamos el envío
                Envio envioGuardado = envioRepository.save(envio);

                // generamos el historial (Auditoría)
                auditoriaService.registrarEvento(
                                envioGuardado,
                                usuarioModificador,
                                eventoRealizado,
                                estadoAnterior,
                                estadoNuevo);
                eventPublisher.publishEvent(new EnvioCambioEstadoEvent(this, envioGuardado, estadoNuevo));

                return envioGuardado;
        }

        // cancelar envio, no permite cancelar a menos que el estado sea pendiente(
        @Transactional
        public Envio cancelarEnvio(String idEnvio, String username) {
                Envio envio = envioRepository.findById(idEnvio)
                                .orElseThrow(() -> new RuntimeException("No se encontró el envío con ID: " + idEnvio));

                // Regla de negocio: Solo cancelar si está pendiente
                if (envio.getEstadoActual() != EstadoEnvio.PENDIENTE) {
                        throw new RuntimeException(
                                        "Validación fallida: No se puede cancelar un envío que ya está en ruta (Estado: "
                                                        + envio.getEstadoActual() + ").");
                }
                Usuario usuarioModificador = usuarioRepository.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

                // Rpara cambiar el estado a CANCELADO
                return actualizarEstadoYPrioridad(idEnvio, "CANCELADO", envio.getPrioridadIa(), usuarioModificador,
                                TipoEvento.CANCELACION);
        }

        @Transactional
        // editarenvio, no permite editar a menos que el estado sea pendiente
        // solo permite cambiar chofer, camion, tipo de grano, prioridad y kg origen
        public Envio editarEnvio(String idEnvio, EnvioRequestDTO dto, String username) {
                Envio envioExistente = envioRepository.findById(idEnvio)
                                .orElseThrow(() -> new RuntimeException("No se encontró el envío con ID: " + idEnvio));

                if (envioExistente.getEstadoActual() != EstadoEnvio.PENDIENTE) {
                        throw new RuntimeException(
                                        "Validación fallida: No se pueden modificar los datos de un viaje que ya comenzó.");
                }
                java.time.LocalDate hoy = java.time.LocalDate.now();
                EstadoEnvio estadoActual = envioExistente.getEstadoActual();

                // actualizacfion selectiva de chofer
                if (dto.getIdChofer() != null) {
                        ChoferDetalle nuevoChofer = choferDetalleRepository.findById(dto.getIdChofer())
                                        .orElseThrow(() -> new RuntimeException("Nuevo chofer no encontrado"));
                        validacionExternaService.verificarLicenciaChofer(hoy, nuevoChofer);
                        envioExistente.setChofer(nuevoChofer);
                } // Si es null, no entra acá y preserva el chofer que ya tenía.

                // actualizacion selectiva de camion
                if (dto.getPatenteCamion() != null && !dto.getPatenteCamion().isBlank()) {
                        Camion nuevoCamion = camionRepository.findById(dto.getPatenteCamion())
                                        .orElseThrow(() -> new RuntimeException("Nuevo camión no encontrado"));
                        validacionExternaService.verificarHabilitacionSenasa(hoy, nuevoCamion);
                        envioExistente.setCamion(nuevoCamion);
                } // Si es null o blank, no entra acá y preserva el camión que ya tenía.

                // 3. actualizacion selectiva de tipó de grano
                if (dto.getTipoGrano() != null) {
                        envioExistente.setTipoGrano(dto.getTipoGrano());
                } // Si es null, no entra acá y preserva el tipo de grano que ya tenía.

                // actualziacion selectiva de prioridad
                if (dto.getPrioridadIa() != null && !dto.getPrioridadIa().isBlank()) {
                        envioExistente.setPrioridadIa(dto.getPrioridadIa());
                } // Si es null o blank, no entra acá y preserva la prioridad que ya tenía.

                // actualñizacion selectiva de kg de origen
                if (dto.getKgOrigen() != null && dto.getKgOrigen() > 0) {
                        envioExistente.setKgOrigen(dto.getKgOrigen());
                } // Si es null o no positivo, no entra acá y preserva los kg origen que ya tenía.

                // Guardamos los cambios consolidados
                Envio envioGuardado = envioRepository.save(envioExistente);

                // Construimos el historial de auditoría
                // Buscamos el usuario operador/supervisor que edita el envio
                Usuario usuarioModificador = usuarioRepository.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("Usuario no encontrado para auditoría"));
                // construimos el historial
                auditoriaService.registrarEvento(
                                envioGuardado,
                                usuarioModificador,
                                TipoEvento.DATOS_ACTUALIZADOS,
                                estadoActual,
                                estadoActual);
                eventPublisher.publishEvent(new EnvioCambioEstadoEvent(this, envioGuardado, estadoActual));
                return envioGuardado;
        }

        @Transactional
        public void asignarChoferCamion(EnvioRequestDTO dto) {
                Envio envio = envioRepository.findById(dto.getIdEnvio())
                                .orElseThrow(() -> new RuntimeException("Envío no encontrado"));
                Camion camion = camionRepository.findById(dto.getPatenteCamion())
                                .orElseThrow(() -> new RuntimeException("Camión no encontrado"));
                ChoferDetalle chofer = choferDetalleRepository.findById(dto.getIdChofer())
                                .orElseThrow(() -> new RuntimeException("Chofer no encontrado"));
                LocalDateTime fechaSalida = LocalDateTime.now();

                envio.setCamion(camion);
                envio.setFechaEstimadaLlegada(trackingService.calcularETA(envio.getDistanciaKm(), fechaSalida));
                envio.setFechaSalida(fechaSalida);
                envio.setChofer(chofer);
                envioRepository.save(envio);

                eventPublisher.publishEvent(new EnvioCambioEstadoEventNotificaciones(this, envio));
        }

        @Transactional(readOnly = true)
        public EnvioDetalleResponseDTO obtenerDetalleConETA(String idEnvio) {
                Envio envio = envioRepository.findById(idEnvio)
                                .orElseThrow(() -> new RuntimeException("No se encontró el envío con ID: " + idEnvio));
                LocalDateTime eta = trackingService.calcularETA(envio.getDistanciaKm(), envio.getFechaSalida());
                return EnvioDetalleResponseDTO.fromEntity(envio, eta);
        }

        private String calcularPrioridad(Envio envio) {
                final int UMBRAL_TONELADAS_KG = 15_000;

                int puntaje = 0;

                if (envio.getKgOrigen() != null && envio.getKgOrigen() > UMBRAL_TONELADAS_KG) {
                        puntaje++;
                }

                TipoGrano grano = envio.getTipoGrano();
                if (grano == TipoGrano.SOJA || grano == TipoGrano.TRIGO) {
                        puntaje++;
                }

                switch (puntaje) {
                        case 2:
                                return "ALTA";
                        case 1:
                                return "MEDIA";
                        default:
                                return "BAJA";
                }
        }

        @Transactional
        public Envio asignarTransporte(String idEnvio, AsignarTransporteDTO dto) {
                List<EstadoEnvio> estadosActivos = Arrays.asList(
                                EstadoEnvio.EN_TRANSITO,
                                EstadoEnvio.EN_PUNTO_DE_RECOLECCION,
                                EstadoEnvio.EN_REPARTO);

                // Verificar que el envío existe
                Envio envio = envioRepository.findById(idEnvio)
                                .orElseThrow(() -> new RuntimeException("No se encontró el envío con ID: " + idEnvio));

                // validaciond e fatiga
                if (evaluacionRepository.existsByEnvioIdEnvioAndEstadoBloqueo(idEnvio,
                                EstadoEvaluacionEnum.RECHAZADO)) {
                        throw new org.springframework.web.server.ResponseStatusException(
                                        org.springframework.http.HttpStatus.FORBIDDEN,
                                        "El viaje no puede iniciar: Existe una evaluación de fatiga rechazada y pendiente de revisión.");
                }

                // Verificar que no tenga ya transporte asignado
                if (envio.getChofer() != null || envio.getCamion() != null) {
                        throw new RuntimeException("El envío ya tiene transporte asignado");
                }

                // Buscar chofer y camión
                ChoferDetalle chofer = choferDetalleRepository.findById(dto.getIdChofer())
                                .orElseThrow(() -> new RuntimeException("Chofer no encontrado"));

                Camion camion = camionRepository.findById(dto.getPatenteCamion())
                                .orElseThrow(() -> new RuntimeException("Camión no encontrado"));

                // Validar licencia y SENASA
                LocalDate hoy = LocalDate.now();
                validacionExternaService.verificarLicenciaChofer(hoy, chofer);
                validacionExternaService.verificarHabilitacionSenasa(hoy, camion);

                // Validar disponibilidad concurrente
                boolean choferOcupado = envioRepository.existsByChoferAndEstadoActualIn(chofer, estadosActivos);
                if (choferOcupado) {
                        throw new RuntimeException(
                                        "El chofer acaba de ser asignado a otro viaje y ya no está disponible.");
                }

                boolean camionOcupado = envioRepository.existsByCamionAndEstadoActualIn(camion, estadosActivos);
                if (camionOcupado) {
                        throw new RuntimeException(
                                        "El camión acaba de ser asignado a otro viaje y ya no está disponible.");
                }

                // Asignar y guardar
                envio.setChofer(chofer);
                envio.setCamion(camion);
                envio.setPrioridadIa(calcularPrioridad(envio)); // envio.setPrioridadIa("ALTA"); hardcodeado por bug .
                envio.setFechaEstimadaLlegada(trackingService.calcularETAConML(envio, camion));
                trackingService.generarYGuardarRuta(envio);

                // Marcar como no disponibles 
                chofer.setDisponible(false);
                camion.setDisponible(false);
                choferDetalleRepository.save(chofer);
                camionRepository.save(camion);

                // creacion autiomarica de evalcuacioon
                EvaluacionPsicomotora nuevaEvaluacion = EvaluacionPsicomotora.builder()
                                .choferId(chofer) 
                                .idEnvio(envio) 
                                .estadoBloqueo(EstadoEvaluacionEnum.ACTIVO)
                                .build();
                evaluacionRepository.save(nuevaEvaluacion);
                eventPublisher.publishEvent(new EnvioCambioEstadoEventNotificaciones(this, envio));

                // Notificacion por mail
                Envio envioGuardado = envioRepository.save(envio);

                return envioRepository.save(envio);

        }

        @Transactional
        public Envio actualizarEstadoOperativo(String idEnvio, EnvioOperativoDTO dto, Authentication auth) {
                Envio envioExistente = envioRepository.findById(idEnvio)
                                .orElseThrow(() -> new RuntimeException("No se encontró el envío con ID: " + idEnvio));

                EstadoEnvio estadoAnterior = envioExistente.getEstadoActual();
                boolean estadoCambiado = (dto.getEstado() != null && dto.getEstado() != estadoAnterior);
                String prioridadFinal = envioExistente.getPrioridadIa();
                boolean prioridadCambiada = false;

                // Validación de Prioridad (Estrictamente restringido a Supervisor)
                if (dto.getPrioridadIa() != null && !dto.getPrioridadIa().equals(envioExistente.getPrioridadIa())) {
                        boolean esSupervisor = auth.getAuthorities().stream()
                                        .anyMatch(a -> a.getAuthority().equals("ROLE_SUPERVISOR"));

                        if (!esSupervisor) {
                                throw new RuntimeException(
                                                "La prioridad del envío solo puede ser modificada por un supervisor.");
                        }
                        prioridadFinal = dto.getPrioridadIa();
                        prioridadCambiada = true;
                }

                Usuario usuarioModificador = usuarioRepository.findByUsername(auth.getName())
                                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

                // Si el estado cambió, delegamos TODO al método centralizado
                if (estadoCambiado) {
                        return actualizarEstadoYPrioridad(
                                        idEnvio,
                                        dto.getEstado().name(),
                                        prioridadFinal,
                                        usuarioModificador,
                                        TipoEvento.CAMBIO_ESTADO);
                }

                // Si NO cambió el estado, pero SÍ cambió la prioridad, guardamos y auditamos
                // acá
                if (prioridadCambiada) {
                        envioExistente.setPrioridadIa(prioridadFinal);
                        Envio envioGuardado = envioRepository.save(envioExistente);

                        auditoriaService.registrarEvento(
                                        envioGuardado,
                                        usuarioModificador,
                                        TipoEvento.CAMBIO_PRIORIDAD,
                                        estadoAnterior,
                                        estadoAnterior,
                                        "Actualización manual de prioridad por usuario autorizado");
                        return envioGuardado;
                }

                // Si no cambió ni el estado ni la prioridad, simplemente retornamos el envío
                // intacto
                return envioExistente;
        }

        @Transactional(readOnly = true)
        public Map<String, Object> obtenerUbicacionActual(String idEnvio) {
                Envio envio = envioRepository.findById(idEnvio)
                                .orElseThrow(() -> new RuntimeException("No se encontró el envío con ID: " + idEnvio));

                return trackingService.calcularUbicacionInterpolada(envio);
        }

        // devuelve la linea entera de la ruta del camion
        @Transactional(readOnly = true)
        public JsonNode obtenerGeometriaRuta(String idEnvio) {
                Envio envio = envioRepository.findById(idEnvio)
                                .orElseThrow(() -> new RuntimeException("No se encontró el envío con ID: " + idEnvio));

                return trackingService.extraerGeometriaRuta(envio.getRutaEnvio());
        }

        public void procesarReasignacion(String viajeId, ReasignacionViajeRequestDTO request, String username) {
                // Buscar el viaje actual
                Envio envio = envioRepository.findById(viajeId)
                                .orElseThrow(() -> new EntityNotFoundException("Viaje no encontrado: " + viajeId));

                // Buscar nuevos recursos
                ChoferDetalle nuevoChofer = choferDetalleRepository.findById(request.getNuevoChoferId().intValue())
                                .orElseThrow(() -> new EntityNotFoundException("Chofer no encontrado"));

                Camion nuevoCamion = camionRepository.findById(request.getNuevoCamionId())
                                .orElseThrow(() -> new EntityNotFoundException("Camión no encontrado"));

                // Validar disponibilidad
                if (!nuevoChofer.getDisponible() || !nuevoCamion.getDisponible()) {
                        throw new IllegalStateException("El chofer o camión seleccionado no está disponible.");
                }

                // Liberar recursos anteriores
                ChoferDetalle choferViejo = envio.getChofer();
                Camion camionViejo = envio.getCamion();

                if (choferViejo != null) {
                        choferViejo.setDisponible(true);

                        List<EvaluacionPsicomotora> evaluacionesPrevias = evaluacionRepository
                                        .buscarEvaluacionesParaDesvincular(
                                                        choferViejo.getIdChofer(),
                                                        viajeId,
                                                        Arrays.asList(EstadoEvaluacionEnum.ACTIVO,
                                                                        EstadoEvaluacionEnum.RECHAZADO));

                        for (EvaluacionPsicomotora eval : evaluacionesPrevias) {
                                eval.setEstadoBloqueo(EstadoEvaluacionEnum.DESVINCULADO_POR_REASIGNACION);
                        }
                        evaluacionRepository.saveAll(evaluacionesPrevias);
                }
                if (camionViejo != null) {
                        camionViejo.setDisponible(true);
                }

                // Asignar nuevos recursos
                envio.setChofer(nuevoChofer);
                envio.setCamion(nuevoCamion);
                nuevoChofer.setDisponible(false);
                nuevoCamion.setDisponible(false);

                // Cambiar estado del viaje
                envio.setEstadoActual(EstadoEnvio.PENDIENTE);

                //Guardar cambios
                envioRepository.save(envio);
                choferDetalleRepository.save(choferViejo);
                choferDetalleRepository.save(nuevoChofer);
                camionRepository.save(camionViejo);
                camionRepository.save(nuevoCamion);

                // registro de auditoria inmutable
                Usuario usuarioModificador = usuarioRepository.findByUsername(username)
                                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado para auditoría"));

                String mensajeAuditoria = "Viaje reasignado. Chofer anterior: " +
                                (choferViejo != null ? choferViejo.getPersonaAsociada().getNombre() : "N/A") +
                                ". Nuevo Chofer: " + nuevoChofer.getPersonaAsociada().getNombre() +
                                ". Motivo: " + request.getMotivoReasignacion();

                auditoriaService.registrarEvento(
                                envio,
                                usuarioModificador,
                                TipoEvento.REASIGNACION, 
                                envio.getEstadoActual(), 
                                EstadoEnvio.PENDIENTE,
                                mensajeAuditoria);

        }

}
