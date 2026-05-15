package com.logitrack.sistema_logistica.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import java.util.Map;

import com.logitrack.sistema_logistica.dto.EnvioRequestDTO;
import com.logitrack.sistema_logistica.dto.HistorialResponseDTO;
import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logitrack.sistema_logistica.dto.EnvioDetalleResponseDTO;
import com.logitrack.sistema_logistica.dto.EnvioOperativoDTO;
import com.logitrack.sistema_logistica.model.Camion;
import com.logitrack.sistema_logistica.model.ChoferDetalle;
import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.model.Establecimiento;
import com.logitrack.sistema_logistica.model.HistorialEstados;
import com.logitrack.sistema_logistica.model.Usuario;
import com.logitrack.sistema_logistica.repository.CamionRepository;
import com.logitrack.sistema_logistica.repository.ChoferDetalleRepository;
import com.logitrack.sistema_logistica.repository.EnvioRepository;
import com.logitrack.sistema_logistica.repository.EnvioSpecifications;
import com.logitrack.sistema_logistica.repository.EstablecimientoRepository;
import com.logitrack.sistema_logistica.repository.HistorialEstadosRepository;
import com.logitrack.sistema_logistica.repository.UsuarioRepository;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import com.logitrack.sistema_logistica.dto.AsignarTransporteDTO;

import com.logitrack.sistema_logistica.model.EmpresaCliente;
import com.logitrack.sistema_logistica.repository.EmpresaClienteRepository;

import org.springframework.security.core.Authentication;

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
        private RestTemplate restTemplate;

        @Transactional // Si algo falla, no se guarda ni el envío ni el historial
        public Envio crearNuevoEnvio(EnvioRequestDTO dto) {
                java.time.LocalDate hoy = java.time.LocalDate.now();
                // validacion CPE
                String nroAutorizacionArca = getNroAutorizacionArca(dto);

                // 1. Buscar todas las relaciones en la Base de Datos
                Establecimiento origen = establecimientoRepository.findById(dto.getIdOrigen())
                                .orElseThrow(() -> new RuntimeException("Establecimiento de origen no encontrado"));
                verificarRucaEmpresa(hoy, origen);

                Establecimiento destino = establecimientoRepository.findById(dto.getIdDestino())
                                .orElseThrow(() -> new RuntimeException("Establecimiento de destino no encontrado"));
                verificarRucaEmpresa(hoy, destino);

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

                // 4. Crear y guardar el Historial inicial
                HistorialEstados historial = HistorialEstados.builder()
                                .envio(nuevoEnvio)
                                .usuario(usuarioCreador)
                                .estadoNuevo(EstadoEnvio.PENDIENTE)
                                // estadoAnterior queda en null porque es el primer estado
                                .build();

                historialEstadosRepository.save(historial);

                // 5. Retornar el envío ya creado
                return nuevoEnvio;
        }

        private void verificarHabilitacionSenasa(java.time.LocalDate hoy, Camion camion) {
                if (camion.getVtoSenasa() != null && camion.getVtoSenasa().isBefore(hoy)) {
                        try {
                                String senasaUrl = "http://localhost:8080/api/mock/senasa/validar-camion/"
                                                + camion.getPatente();
                                ResponseEntity<Map> responseSenasa = restTemplate.getForEntity(senasaUrl, Map.class);

                                if (responseSenasa.getStatusCode().is2xxSuccessful()
                                                && responseSenasa.getBody() != null) {
                                        String vtoSenasaStr = (String) responseSenasa.getBody()
                                                        .get("vencimientoHabilitacion");

                                        camion.setVtoSenasa(java.time.LocalDate.parse(vtoSenasaStr));
                                        camionRepository.save(camion);
                                }
                        } catch (HttpClientErrorException e) {
                                throw new RuntimeException(
                                                "Validación SENASA rechazada: El camión no está habilitado para transporte de granos.");
                        } catch (Exception e) {
                                throw new RuntimeException("Error de conexión al validar con SENASA.");
                        }
                }
        }

        private void verificarRucaEmpresa(java.time.LocalDate hoy, Establecimiento origen) {
                EmpresaCliente empresa = origen.getEmpresa();

                if (empresa != null && empresa.getVtoRuca() != null && empresa.getVtoRuca().isBefore(hoy)) {
                        try {
                                String rucaUrl = "http://localhost:8080/api/mock/ruca/validar-empresa/"
                                                + empresa.getRucaNro();
                                ResponseEntity<Map> responseRuca = restTemplate.getForEntity(rucaUrl, Map.class);

                                if (responseRuca.getStatusCode().is2xxSuccessful() && responseRuca.getBody() != null) {
                                        String vtoRucaStr = (String) responseRuca.getBody().get("vtoRucaNuevo");

                                        empresa.setVtoRuca(java.time.LocalDate.parse(vtoRucaStr));
                                        empresa.setVtoRuca(java.time.LocalDate.parse(vtoRucaStr));
                                        empresaClienteRepository.save(empresa);
                                }
                        } catch (HttpClientErrorException e) {
                                throw new RuntimeException(
                                                "Validación RUCA rechazada: La empresa dueña del origen está suspendida.");
                        } catch (Exception e) {
                                throw new RuntimeException("Error de conexión al validar RUCA.");
                        }
                }
        }

        private void verificarLicenciaChofer(java.time.LocalDate hoy, ChoferDetalle chofer) {
                if (chofer.getVtoLicencia().isBefore(hoy) || chofer.getVtoLinti().isBefore(hoy)) {
                        try {
                                String cnrtUrl = "http://localhost:8080/api/mock/cnrt/validar-chofer/"
                                                + chofer.getNroLicencia();
                                ResponseEntity<Map> responseCnrt = restTemplate.getForEntity(cnrtUrl, Map.class);

                                if (responseCnrt.getStatusCode().is2xxSuccessful() && responseCnrt.getBody() != null) {
                                        String vtoLicenciaStr = (String) responseCnrt.getBody()
                                                        .get("vtoLicenciaNuevo");
                                        String vtoLintiStr = (String) responseCnrt.getBody().get("vtoLintiNuevo");

                                        // Actualizamos nuestra base de datos local
                                        chofer.setVtoLicencia(java.time.LocalDate.parse(vtoLicenciaStr));
                                        chofer.setVtoLinti(java.time.LocalDate.parse(vtoLintiStr));
                                        choferDetalleRepository.save(chofer);
                                }
                        } catch (HttpClientErrorException e) {
                                throw new RuntimeException(
                                                "La CNRT informa que el chofer está inhabilitado para conducir.");
                        } catch (Exception e) {
                                throw new RuntimeException("Error al validar con CNRT.");
                        }
                }
        }

        private String getNroAutorizacionArca(EnvioRequestDTO dto) {
                String nroAutorizacionArca = "";
                try {
                        String arcaUrl = "http://localhost:8080/api/mock/arca/validar-cpe/" + dto.getCpe();
                        ResponseEntity<Map> response = restTemplate.getForEntity(arcaUrl, Map.class);

                        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                                nroAutorizacionArca = (String) response.getBody().get("nroAutorizacion");
                        }
                } catch (HttpClientErrorException e) {
                        throw new RuntimeException("Validación ARCA rechazada: El CPE es inválido o está inactivo.");
                } catch (Exception e) {
                        throw new RuntimeException("Error de conexión con el servicio de ARCA.");
                }
                return nroAutorizacionArca;
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
                return actualizarEstadoYPrioridad(idEnvio, nuevoEstadoStr, envio.getPrioridadIa(), usuario);
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
                        Usuario usuarioModificador) {

                // 1. Buscamos el envío nuevamente para asegurar consistencia
                Envio envio = envioRepository.findById(idEnvio)
                                .orElseThrow(() -> new RuntimeException("No se encontró el envío con ID: " + idEnvio));

                EstadoEnvio estadoAnterior = envio.getEstadoActual();
                EstadoEnvio estadoNuevo = EstadoEnvio.valueOf(nuevoEstadoStr);

                // 2. Actualizamos los campos en la entidad
                envio.setEstadoActual(estadoNuevo);
                envio.setPrioridadIa(nuevaPrioridad); // Aquí el chofer mantiene la que ya tenía

                // 3. Guardamos el envío
                Envio envioGuardado = envioRepository.save(envio);

                // 4. GENERAMOS EL HISTORIAL (Auditoría)
                HistorialEstados historial = HistorialEstados.builder()
                                .envio(envioGuardado)
                                .usuario(usuarioModificador)
                                .estadoAnterior(estadoAnterior)
                                .estadoNuevo(estadoNuevo)
                                .build();

                historialEstadosRepository.save(historial);

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
                return actualizarEstadoYPrioridad(idEnvio, "CANCELADO", envio.getPrioridadIa(), usuarioModificador);
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

                ChoferDetalle nuevoChofer = choferDetalleRepository.findById(dto.getIdChofer())
                                .orElseThrow(() -> new RuntimeException("Nuevo chofer no encontrado"));
                verificarLicenciaChofer(hoy, nuevoChofer);

                Camion nuevoCamion = camionRepository.findById(dto.getPatenteCamion())
                                .orElseThrow(() -> new RuntimeException("Nuevo camión no encontrado"));
                verificarHabilitacionSenasa(hoy, nuevoCamion);

                envioExistente.setChofer(nuevoChofer);
                envioExistente.setCamion(nuevoCamion);
                envioExistente.setTipoGrano(dto.getTipoGrano());
                envioExistente.setPrioridadIa(dto.getPrioridadIa());
                envioExistente.setKgOrigen(dto.getKgOrigen());

                return envioRepository.save(envioExistente);
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
                envio.setFechaEstimadaLlegada(calcularETA(envio.getDistanciaKm(), fechaSalida));
                envio.setFechaSalida(fechaSalida);
                envio.setChofer(chofer);
                envioRepository.save(envio);

        }

        private static final double VELOCIDAD_PROMEDIO_KMH = 65.0;

        // private LocalDateTime calcularETA(Double distanciaKm, LocalDateTime
        // fecha_salida) {
        // if (distanciaKm == null || distanciaKm <= 0) {
        // return null;
        // }
        // long minutosViaje = Math.round((distanciaKm / VELOCIDAD_PROMEDIO_KMH) * 60);
        // return fechaSalida.plusMinutes(minutosViaje);
        // }
        private LocalDateTime calcularETA(Double distanciaKm, LocalDateTime fechaSalida) {
                if (distanciaKm == null || distanciaKm <= 0 || fechaSalida == null) {
                        return null;
                }
                long minutosViaje = Math.round((distanciaKm / VELOCIDAD_PROMEDIO_KMH) * 60);
                return fechaSalida.plusMinutes(minutosViaje);
        }

        /**
         * #122 — OBTENER DETALLE CON ETA
         * Usado por el endpoint GET /api/envios/{id}
         */

        @Transactional(readOnly = true)
        public EnvioDetalleResponseDTO obtenerDetalleConETA(String idEnvio) {
                Envio envio = envioRepository.findById(idEnvio)
                                .orElseThrow(() -> new RuntimeException("No se encontró el envío con ID: " + idEnvio));

                LocalDateTime eta = calcularETA(envio.getDistanciaKm(), envio.getFechaSalida());

                return EnvioDetalleResponseDTO.fromEntity(envio, eta);
        }

        @Transactional
        public Envio asignarTransporte(String idEnvio, AsignarTransporteDTO dto) {
                // 1. Verificar que el envío existe
                Envio envio = envioRepository.findById(idEnvio)
                                .orElseThrow(() -> new RuntimeException("No se encontró el envío con ID: " + idEnvio));

                // 2. Verificar que no tenga ya transporte asignado
                if (envio.getChofer() != null || envio.getCamion() != null) {
                        throw new RuntimeException("El envío ya tiene transporte asignado");
                }

                // 3. Buscar chofer y camión — ambos obligatorios
                ChoferDetalle chofer = choferDetalleRepository.findById(dto.getIdChofer())
                                .orElseThrow(() -> new RuntimeException("Chofer no encontrado"));

                Camion camion = camionRepository.findById(dto.getPatenteCamion())
                                .orElseThrow(() -> new RuntimeException("Camión no encontrado"));

                // 4. Asignar y guardar
                envio.setChofer(chofer);
                envio.setCamion(camion);

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

                        HistorialEstados historial = HistorialEstados.builder()
                                        .envio(envioGuardado)
                                        .usuario(usuarioModificador)
                                        .estadoAnterior(estadoAnterior)
                                        .estadoNuevo(envioGuardado.getEstadoActual())
                                        .build();

                        historialEstadosRepository.save(historial);
                }

                return envioGuardado;
        }

}
