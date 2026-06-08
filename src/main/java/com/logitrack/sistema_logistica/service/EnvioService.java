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
        import com.logitrack.sistema_logistica.events.EnvioCambioEstadoEvent;
        import com.logitrack.sistema_logistica.events.EnvioNuevoEvent;
        import com.logitrack.sistema_logistica.model.Camion;
        import com.logitrack.sistema_logistica.model.ChoferDetalle;
        import com.logitrack.sistema_logistica.model.Envio;
        import com.logitrack.sistema_logistica.model.Establecimiento;
        import com.logitrack.sistema_logistica.model.Usuario;
        import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;
        import com.logitrack.sistema_logistica.model.enums.TipoEvento;
        import com.logitrack.sistema_logistica.repository.CamionRepository;
        import com.logitrack.sistema_logistica.repository.ChoferDetalleRepository;
        import com.logitrack.sistema_logistica.repository.EmpresaClienteRepository;
        import com.logitrack.sistema_logistica.repository.EnvioRepository;
        import com.logitrack.sistema_logistica.repository.EnvioSpecifications;
        import com.logitrack.sistema_logistica.repository.EstablecimientoRepository;
        import com.logitrack.sistema_logistica.repository.HistorialEstadosRepository;
        import com.logitrack.sistema_logistica.repository.RutaEnvioRepository;
        import com.logitrack.sistema_logistica.repository.UsuarioRepository;

        @Service
        public class EnvioService {

                @Autowired
                private HistorialEstadosRepository historialRepository;
                @Autowired
                private EnvioRepository envioRepository;
                @Autowired
                private EstablecimientoRepository establecimientoRepository;
                @Autowired
                private ChoferDetalleRepository choferDetalleRepository;
                @Autowired
                private CamionRepository camionRepository;
                @Autowired
                private HistorialEstadosRepository historialEstadosRepository;
                @Autowired
                private UsuarioRepository usuarioRepository;
                @Autowired
                private EmpresaClienteRepository empresaClienteRepository;

                @Autowired
                private ValidacionExternaService validacionExternaService;
                @Autowired
                private TrackingGeospatialService trackingService;

                @Autowired
                private GraphHopperService graphHopperService;
                @Autowired
                private RutaEnvioRepository rutaEnvioRepository;

                @Autowired 
                private AuditoriaService auditoriaService;

                @Autowired
                private ApplicationEventPublisher eventPublisher;



                @Transactional // Si algo falla, no se guarda ni el envío ni el historial
                public Envio crearNuevoEnvio(EnvioRequestDTO dto) {
                        java.time.LocalDate hoy = java.time.LocalDate.now();
                        // validacion CPE
                        String nroAutorizacionArca = validacionExternaService.getNroAutorizacionArca(dto.getCpe());

                        // 1. Buscar todas las relaciones en la Base de Datos
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

                        // 2. Construir el objeto Envio
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

                        // 3. Guardar el Envío (Acá se autogenera el id "LT-XXXXXX" y la fecha)
                        nuevoEnvio = envioRepository.save(nuevoEnvio);
                        eventPublisher.publishEvent(new EnvioNuevoEvent(this, nuevoEnvio));

                        // 4. Crear y guardar el Historial inicial
                        auditoriaService.registrarEvento(
                                                nuevoEnvio, 
                                                usuarioCreador, 
                                                TipoEvento.CREACION, 
                                                null, 
                                                EstadoEnvio.PENDIENTE
                                        );

                        // 5. Retornar el envío ya creado
                        return nuevoEnvio;
                }

                // que pasa si el envío existe o si no se encuentra.
                public Envio buscarPorId(String idEnvio) {
                        return envioRepository.buscarPorId(idEnvio)
                                        .orElseThrow(() -> new RuntimeException(
                                                        "No se encontró el envío con el idEnvio: " + idEnvio));
                }

                public Page<Envio> buscarEnviosConFiltros(EstadoEnvio estado, LocalDateTime fechaInicio,
                                LocalDateTime fechaFin, String termino, String tipoGrano, Pageable pageable) {
                        Specification<Envio> spec = Specification.where(EnvioSpecifications.tieneEstado(estado))
                                        .and(EnvioSpecifications.fechaCreacionEntre(fechaInicio, fechaFin))
                                        .and(EnvioSpecifications.contieneTermino(termino))
                                        .and(EnvioSpecifications.esDeTipoGrano(tipoGrano));
                        return envioRepository.findAll(spec, pageable);
                }

                // #113
                // Lógica de obtención
                // Conecta la identidad del usuario con la base de datos.
                public List<Envio> obtenerEnviosPorChofer(String username) {
                        return envioRepository.findByChoferUsername(username);
                }

                // #114: Actualización de estado por parte del chofer con validaciones estrictas
                @Transactional
                public Envio actualizarEstadoChofer(String idEnvio, String nuevoEstadoStr, String username) {
                        // 1. Buscar el envío
                        Envio envio = envioRepository.findById(idEnvio)
                                        .orElseThrow(() -> new RuntimeException("Envío no encontrado"));

                        // 2. Validación de Identidad: ¿Es su envío asignado?
                        String usernameAsignado = envio.getChofer().getPersonaAsociada().getIdUsuario().getUsername();
                        if (!usernameAsignado.equals(username)) {
                                throw new RuntimeException("Acceso denegado: Este envío no te pertenece");
                        }

                        // 3. Máquina de Estados: Validar flujo lógico [cite: 49, 111]
                        EstadoEnvio actual = envio.getEstadoActual();
                        EstadoEnvio siguiente = EstadoEnvio.valueOf(nuevoEstadoStr);

                        // NUEVO: Si el estado es el mismo, no hacemos nada y devolvemos el envío tal
                        // cual
                        if (actual == siguiente) {
                                return envio;
                        }

                        if (!esTransicionValida(actual, siguiente)) {
                                throw new RuntimeException(
                                                "Flujo inválido: No se puede pasar de " + actual + " a " + siguiente);
                        }

                        // 4. Actualizar (Manteniendo la prioridad intacta)
                        Usuario usuario = usuarioRepository.findByUsername(username).get();
                        return actualizarEstadoYPrioridad(idEnvio, nuevoEstadoStr, envio.getPrioridadIa(), usuario,
                                        TipoEvento.CAMBIO_ESTADO);
                }

                /**
                 * Obtiene el historial de eventos de un envío por su identificador.
                 * Primero valida que el envío exista y luego devuelve los registros de
                 * historial
                 * transformados a DTO para exponer solo los campos necesarios.
                 */
                @Transactional(readOnly = true)
                public List<HistorialResponseDTO> obtenerHistorialPorEnvio(String idEnvio) {
                        // Validar existencia del envío antes de consultar el historial
                        if (!envioRepository.existsById(idEnvio)) {
                                throw new RuntimeException("No se encontró el envío con idEnvio: " + idEnvio);
                        }

                        // Buscar los registros de historial ordenados por fecha descendente
                        return auditoriaService.obtenerHistorialPorEnvio(idEnvio);
                }

                /**
                 * #114: Validación de Flujo Lógico para Actualización de Estado
                 * Método de apoyo: Valida que el chofer siga el flujo lógico
                 * sin saltarse pasos ni retroceder.
                 */
                private boolean esTransicionValida(EstadoEnvio actual, EstadoEnvio siguiente) {
                        return switch (actual) {
                                case PENDIENTE -> siguiente == EstadoEnvio.EN_TRANSITO;
                                case EN_TRANSITO -> siguiente == EstadoEnvio.EN_PUNTO_DE_RECOLECCION;
                                case EN_PUNTO_DE_RECOLECCION -> siguiente == EstadoEnvio.EN_REPARTO;
                                case EN_REPARTO -> siguiente == EstadoEnvio.ENTREGADO;
                                default -> false; // El chofer no puede cancelar ni modificar estados finales
                        };
                }

                /**
                 * #114: Método centralizado para actualizar el estado y la prioridad de un
                 * envío,
                 * Método de apoyo: Centraliza la actualización del envío y
                 * la creación automática del registro de historial.
                 */
                @Transactional
                public Envio actualizarEstadoYPrioridad(String idEnvio, String nuevoEstadoStr, String nuevaPrioridad,
                        Usuario usuarioModificador, TipoEvento eventoRealizado) {

                        // 1. Buscamos el envío nuevamente para asegurar consistencia
                        Envio envio = envioRepository.findById(idEnvio)
                                        .orElseThrow(() -> new RuntimeException("No se encontró el envío con ID: " + idEnvio));
                                        
                        EstadoEnvio estadoAnterior = envio.getEstadoActual();
                        EstadoEnvio estadoNuevo = EstadoEnvio.valueOf(nuevoEstadoStr);

                        // logica de ruteo
                        // Si el estado cambia a En transito o Enreparto , pedimos la ruta
                        if (estadoNuevo == EstadoEnvio.EN_TRANSITO || estadoNuevo == EstadoEnvio.EN_PUNTO_DE_RECOLECCION) {
                                        trackingService.generarYGuardarRuta(envio);
                                }

                        // 2. Actualizamos los campos en la entidad
                        envio.setEstadoActual(estadoNuevo);
                        envio.setPrioridadIa(nuevaPrioridad); // Aquí el chofer mantiene la que ya tenía

                        //#222 - Liberamos a los choferes y camiónes si el envío termina o se cancela
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
                        // 3. Guardamos el envío
                        Envio envioGuardado = envioRepository.save(envio);

                        // 4. GENERAMOS EL HISTORIAL (Auditoría)
                        auditoriaService.registrarEvento(
                                                envioGuardado, 
                                                usuarioModificador, 
                                                eventoRealizado, 
                                                estadoAnterior, 
                                                estadoNuevo
                                        );
                eventPublisher.publishEvent(new EnvioCambioEstadoEvent(this, envioGuardado, estadoNuevo));

                return envioGuardado;
        }


        // cancelar envio, no permite cancelar a menos que el estado sea pendiente(esto
        // lo podemos cambiar despues)
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

                // Reutilizamos tu método centralizado para cambiar el estado a CANCELADO
                // Nota: Asegurate de tener CANCELADO en tu Enum EstadoEnvio
                return actualizarEstadoYPrioridad(idEnvio, "CANCELADO", envio.getPrioridadIa(), usuarioModificador,
                                TipoEvento.CANCELACION);
        }

        @Transactional
        // editarenvio, no permite editar a menos que el estado sea pendiente(esto lo
        // podemos cambiar despues)
        // solo permite cambiar chofer, camion, tipo de grano, prioridad y kg origen
        // si hay que cambiar origwn o destino, se cancela el envio y se hace uno nuevo
        public Envio editarEnvio(String idEnvio, EnvioRequestDTO dto, String username) {
                Envio envioExistente = envioRepository.findById(idEnvio)
                                .orElseThrow(() -> new RuntimeException("No se encontró el envío con ID: " + idEnvio));

                if (envioExistente.getEstadoActual() != EstadoEnvio.PENDIENTE) {
                        throw new RuntimeException(
                                        "Validación fallida: No se pueden modificar los datos de un viaje que ya comenzó.");
                }
                java.time.LocalDate hoy = java.time.LocalDate.now();
                EstadoEnvio estadoActual = envioExistente.getEstadoActual();

                // 1. ACTUALIZACIÓN SELECTIVA DE CHOFER
                if (dto.getIdChofer() != null) {
                        ChoferDetalle nuevoChofer = choferDetalleRepository.findById(dto.getIdChofer())
                        .orElseThrow(() -> new RuntimeException("Nuevo chofer no encontrado"));
                        validacionExternaService.verificarLicenciaChofer(hoy, nuevoChofer);
                        envioExistente.setChofer(nuevoChofer);
                }// Si es null, no entra acá y preserva el chofer que ya tenía.

                // 2. ACTUALIZACIÓN SELECTIVA DE CAMIÓN
                if (dto.getPatenteCamion() != null && !dto.getPatenteCamion().isBlank()) {
                        Camion nuevoCamion = camionRepository.findById(dto.getPatenteCamion())
                        .orElseThrow(() -> new RuntimeException("Nuevo camión no encontrado"));
                validacionExternaService.verificarHabilitacionSenasa(hoy, nuevoCamion);
                envioExistente.setCamion(nuevoCamion);
                } // Si es null o blank, no entra acá y preserva el camión que ya tenía.
                
                // 3. ACTUALIZACIÓN SELECTIVA DE TIPO DE GRANO
                if (dto.getTipoGrano() != null) {
                        envioExistente.setTipoGrano(dto.getTipoGrano());
                }// Si es null, no entra acá y preserva el tipo de grano que ya tenía.  
                
                // 4. ACTUALIZACIÓN SELECTIVA DE PRIORIDAD
                if (dto.getPrioridadIa() != null && !dto.getPrioridadIa().isBlank()) {
                        envioExistente.setPrioridadIa(dto.getPrioridadIa());
                } // Si es null o blank, no entra acá y preserva la prioridad que ya tenía.

                // 5. ACTUALIZACIÓN SELECTIVA DE KILOS ORIGEN
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
                                        estadoActual
                );
                return envioGuardado;
        }

        /*
        * #121: Método calcular el ETA (Tiempo Estimado de Llegada) de un envío,
        * Velocidad promedio fija: 65 km/h
        */

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

        }


        /**
         * #122 — OBTENER DETALLE CON ETA
         * Usado por el endpoint GET /api/envios/{id}
         */

        @Transactional(readOnly = true)
        public EnvioDetalleResponseDTO obtenerDetalleConETA(String idEnvio) {
                Envio envio = envioRepository.findById(idEnvio)
                                .orElseThrow(() -> new RuntimeException("No se encontró el envío con ID: " + idEnvio));

                LocalDateTime eta = trackingService.calcularETA(envio.getDistanciaKm(), envio.getFechaSalida());

                return EnvioDetalleResponseDTO.fromEntity(envio, eta);
        }

        @Transactional
        public Envio asignarTransporte(String idEnvio, AsignarTransporteDTO dto) {

        List<EstadoEnvio> estadosActivos = Arrays.asList(
                EstadoEnvio.EN_TRANSITO,
                EstadoEnvio.EN_PUNTO_DE_RECOLECCION,
                EstadoEnvio.EN_REPARTO
        );

        // 1. Verificar que el envío existe
        Envio envio = envioRepository.findById(idEnvio)
                .orElseThrow(() -> new RuntimeException("No se encontró el envío con ID: " + idEnvio));

        // 2. Verificar que no tenga ya transporte asignado
        if (envio.getChofer() != null || envio.getCamion() != null) {
                throw new RuntimeException("El envío ya tiene transporte asignado");
        }

        // 3. Buscar chofer y camión
        ChoferDetalle chofer = choferDetalleRepository.findById(dto.getIdChofer())
                .orElseThrow(() -> new RuntimeException("Chofer no encontrado"));

        Camion camion = camionRepository.findById(dto.getPatenteCamion())
                .orElseThrow(() -> new RuntimeException("Camión no encontrado"));

        // 4. Validar licencia y SENASA
        LocalDate hoy = LocalDate.now();
        validacionExternaService.verificarLicenciaChofer(hoy, chofer);
        validacionExternaService.verificarHabilitacionSenasa(hoy, camion);

        // 5. Validar disponibilidad concurrente (#213)
        boolean choferOcupado = envioRepository.existsByChoferAndEstadoActualIn(chofer, estadosActivos);
        if (choferOcupado) {
                throw new RuntimeException("El chofer acaba de ser asignado a otro viaje y ya no está disponible.");
        }

        boolean camionOcupado = envioRepository.existsByCamionAndEstadoActualIn(camion, estadosActivos);
        if (camionOcupado) {
                throw new RuntimeException("El camión acaba de ser asignado a otro viaje y ya no está disponible.");
        }

        // 6. Asignar y guardar
        envio.setChofer(chofer);
        envio.setCamion(camion);
        envio.setEstadoActual(estadosActivos.get(0));
        envio.setPrioridadIa("ALTA");//hardcodeado por bug .
        trackingService.generarYGuardarRuta(envio);


        // 7. Marcar como no disponibles (#222)
        chofer.setDisponible(false);
        camion.setDisponible(false);
        choferDetalleRepository.save(chofer);
        camionRepository.save(camion);

        //Notificacion por mail
        Envio envioGuardado = envioRepository.save(envio);
        eventPublisher.publishEvent(
        new EnvioCambioEstadoEvent(this, envioGuardado, EstadoEnvio.EN_TRANSITO));

        return envioRepository.save(envio);
        }

        // SOLUCIÓN TEMPORAL para editar los estados de un envío desde la vista de
        // operador/supervisor
        @Transactional
        public Envio actualizarEstadoOperativo(String idEnvio, EnvioOperativoDTO dto, Authentication auth) {
                Envio envioExistente = envioRepository.findById(idEnvio)
                                .orElseThrow(() -> new RuntimeException("No se encontró el envío con ID: " + idEnvio));

                EstadoEnvio estadoAnterior = envioExistente.getEstadoActual();
                boolean estadoCambiado = false;

                // 1. Actualización de Estado (Permitido para Operador y Supervisor)
                if (dto.getEstado() != null && dto.getEstado() != estadoAnterior) {
                        envioExistente.setEstadoActual(dto.getEstado());
                        estadoCambiado = true;
                }

                // 2. Actualización de Prioridad (Estrictamente restringido a Supervisor)
                if (dto.getPrioridadIa() != null && !dto.getPrioridadIa().equals(envioExistente.getPrioridadIa())) {
                        boolean esSupervisor = auth.getAuthorities().stream()
                                        .anyMatch(a -> a.getAuthority().equals("ROLE_SUPERVISOR"));

                        if (!esSupervisor) {
                                throw new RuntimeException(
                                                "La prioridad del envío solo puede ser modificada por un supervisor.");
                        }
                        envioExistente.setPrioridadIa(dto.getPrioridadIa());
                }

                Envio envioGuardado = envioRepository.save(envioExistente);

                // 3. Generar el historial solo si el estado realmente cambió
                if (estadoCambiado) {
                        Usuario usuarioModificador = usuarioRepository.findByUsername(auth.getName())
                                        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

                        /*
                        * HistorialEstados historial = HistorialEstados.builder()
                        * .envio(envioGuardado)
                        * .usuario(usuarioModificador)
                        * .estadoAnterior(estadoAnterior)
                        * .estadoNuevo(envioGuardado.getEstadoActual())
                        * .build();
                        * 
                        * historialEstadosRepository.save(historial);
                        */
                        return actualizarEstadoYPrioridad(
                                        idEnvio,
                                        dto.getEstado().name(),
                                        envioExistente.getPrioridadIa(),
                                        usuarioModificador,
                                        TipoEvento.CAMBIO_ESTADO);
                }

                // ... (lógica anterior de actualizarEstadoOperativo)

                // Si el estado no cambió, verificamos si AL MENOS cambió la prioridad para
                // auditarlo
                if (!estadoCambiado && dto.getPrioridadIa() != null
                        && !dto.getPrioridadIa().equals(estadoAnterior.name())) {
                Usuario usuarioModificador = usuarioRepository.findByUsername(auth.getName()).get();

                auditoriaService.registrarEvento(
                        envioGuardado, 
                        usuarioModificador, 
                        TipoEvento.CAMBIO_PRIORIDAD, 
                        estadoAnterior, 
                        estadoAnterior
                );
        }

                return envioGuardado;
        }

        // Calcula y devuelve la ubicación exacta del camión en base al tiempo
        // transcurrido
        // y la ruta generada por GraphHopper.

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
        

}
