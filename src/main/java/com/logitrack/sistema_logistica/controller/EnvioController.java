package com.logitrack.sistema_logistica.controller;

import com.logitrack.sistema_logistica.dto.ErrorResponseDTO;
import com.logitrack.sistema_logistica.dto.EstadoUpdateRequestDTO;
import com.logitrack.sistema_logistica.dto.EstadoUpdateResponseDTO;
import com.logitrack.sistema_logistica.dto.EnvioRequestDTO;
import com.logitrack.sistema_logistica.dto.HistorialResponseDTO;
import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.model.Usuario;
import com.logitrack.sistema_logistica.model.enums.Estado_Envio;
import com.logitrack.sistema_logistica.service.EnvioService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.logitrack.sistema_logistica.repository.EnvioRepository;
import com.logitrack.sistema_logistica.repository.UsuarioRepository;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/envios")
public class EnvioController {

    @Autowired
    private EnvioService envioService;

    @Autowired
    private EnvioRepository envioRepository;

    @Autowired
    private UsuarioRepository usuarioRepository; // Inyectar repositorio -> Necesario para no enviar el ID de usuario
                                                 // desde el
                                                 // frontend, sino que el EnvioController extraiga quién es el usuario
                                                 // directamente leyendo el Token JWT de la petición. El usuario es
                                                 // necesario para auditorias.

    // GET para listar (siempre es útil tenerlo)
    @GetMapping
    public List<Envio> listarEnvios() {
        return envioRepository.findAll();
    }

    // GET para buscar envíos con filtros opcionales por fecha, estado y paginación
    @GetMapping("/search")
    public ResponseEntity<?> buscarEnvios(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String fecha,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            LocalDate fechaFiltro = null;
            Estado_Envio estadoFiltro = null;

            // Parsear los parámetros
            if (fecha != null && !fecha.isBlank()) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                fechaFiltro = LocalDate.parse(fecha, formatter);
            }

            if (estado != null && !estado.isBlank()) {
                estadoFiltro = Estado_Envio.valueOf(estado.toUpperCase());
            }

            LocalDateTime fechaInicio = null;
            LocalDateTime fechaFin = null;
            if (fechaFiltro != null) {
                fechaInicio = fechaFiltro.atStartOfDay();
                fechaFin = fechaFiltro.plusDays(1).atStartOfDay();
            }

            String termino = (query != null && !query.isBlank()) ? query.trim() : null;
            Pageable pageable = PageRequest.of(page, size);
            Page<Envio> envios = envioService.buscarEnviosConFiltros(estadoFiltro, fechaInicio, fechaFin, termino,
                    pageable);
            return ResponseEntity.ok(envios);
        } catch (DateTimeParseException e) {
            ErrorResponseDTO error = new ErrorResponseDTO();
            error.setMessage("Formato de fecha inválido. Use dd/MM/yyyy.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (IllegalArgumentException e) {
            ErrorResponseDTO error = new ErrorResponseDTO();
            error.setMessage(
                    "Estado inválido. Use uno de los valores permitidos: PENDIENTE, EN_TRANSITO, ENTREGADO, CANCELADO");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // GET para obtener el historial de un envío por su identificador
    @GetMapping("/{idEnvio}/historial")
    public ResponseEntity<?> consultarHistorial(@PathVariable String idEnvio) {
        try {
            // Llamar al servicio que valida el envío y devuelve los eventos ya
            // transformados a DTO
            List<HistorialResponseDTO> historial = envioService.obtenerHistorialPorEnvio(idEnvio);
            return ResponseEntity.ok(historial);
        } catch (RuntimeException e) {
            // Responder con 404 cuando el envío no exista
            ErrorResponseDTO error = new ErrorResponseDTO();
            error.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            // Responder con 500 para errores inesperados
            ErrorResponseDTO error = new ErrorResponseDTO();
            error.setMessage("Error al obtener el historial: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // // POST para crear
    // @PostMapping
    // public ResponseEntity<?> crearEnvio(@RequestBody EnvioRequestDTO dto) {
    // try {
    // Envio envioCreado = envioService.crearNuevoEnvio(dto);
    // return new ResponseEntity<>(envioCreado, HttpStatus.CREATED);
    // } catch (RuntimeException e) {
    // // Si falla una validación (ej: camión no existe), devolvemos un 400 Bad
    // Request
    // return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    // }
    // }

    // Nuevo POST para crear envío. Necesario para no enviar el ID de usuario desde
    // el
    // frontend, sino que el EnvioController extraiga quién es el usuario
    // directamente leyendo el Token JWT de la petición. El ID de usuario es
    // necesario
    // para auditorias.
    @PostMapping
    // Se agrega el parámetro Authentication.
    public ResponseEntity<?> crearEnvio(@RequestBody EnvioRequestDTO dto, Authentication authentication) {
        try {
            // Extraer el email/username del token JWT
            String username = authentication.getName();

            // Buscar el ID del usuario en la Base de Datos
            Usuario usuario = usuarioRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuario autenticado no existe en el sistema"));

            // Asignarlo al DTO de forma segura antes de guardarlo
            dto.setId_usuario_creador(usuario.getId_usuario());

            Envio envioCreado = envioService.crearNuevoEnvio(dto);
            return new ResponseEntity<>(envioCreado, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // El Front va a llamar cuando el usuario escriba en la barra de búsqueda.
    @GetMapping("/buscar/{id_envio}")
    public ResponseEntity<?> obtenerEnvioPorTracking(@PathVariable String id_envio) {
        try {
            Envio envio = envioService.buscarPorId(id_envio);
            return ResponseEntity.ok(envio);
        } catch (RuntimeException e) {

            // reamos la instancia vacía
            ErrorResponseDTO error = new ErrorResponseDTO();

            // Le cargamos el mensaje
            error.setMessage(e.getMessage());

            // Aprovechamos el ErrorResponseDTO que ya habiamos creado
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    // Lo siguiente se agregó como recomendación de Gemini para
    // cumplir con las funciones que tiene el front.

    // ─── DTO INTERNO PARA ACTUALIZACIONES ───
    public static class UpdateEnvioDTO {
        private String estado;
        private String prioridad;

        public String getEstado() {
            return estado;
        }

        public void setEstado(String estado) {
            this.estado = estado;
        }

        public String getPrioridad() {
            return prioridad;
        }

        public void setPrioridad(String prioridad) {
            this.prioridad = prioridad;
        }
    }

    // ─── GET: BUSCAR POR ID INTERNO (LT-XXXXXX) ───
    @GetMapping("/{idEnvio}")
    public ResponseEntity<?> obtenerEnvioPorId(@PathVariable String idEnvio) {
        try {
            // Utilizamos el repository inyectado para buscar por su ID principal
            Envio envio = envioRepository.findById(idEnvio)
                    .orElseThrow(() -> new RuntimeException("El envío no existe en la base de datos"));
            return ResponseEntity.ok(envio);
        } catch (RuntimeException e) {
            ErrorResponseDTO error = new ErrorResponseDTO();
            error.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /*
     * // ─── PUT: ACTUALIZAR ESTADO Y PRIORIDAD ───
     * 
     * @PutMapping("/{idEnvio}")
     * public ResponseEntity<?> actualizarEnvio(
     * 
     * @PathVariable String idEnvio,
     * 
     * @RequestBody UpdateEnvioDTO dto,
     * Authentication authentication) {
     * try {
     * // 1. Identificar al usuario que hace el cambio
     * String username = authentication.getName();
     * Usuario usuario = usuarioRepository.findByUsername(username)
     * .orElseThrow(() -> new RuntimeException("Usuario autenticado no válido"));
     * 
     * // 2. Aquí llamamos a tu Servicio.
     * // NOTA PARA TI: En tu EnvioService.java deberás crear este método.
     * // Ese método debe buscar el envío, comparar los estados, crear el registro
     * en
     * // Historial_Estados con el 'usuario' capturado y guardar los cambios.
     * Envio envioActualizado = envioService.actualizarEstadoYPrioridad(
     * idEnvio,
     * dto.getEstado(),
     * dto.getPrioridad(),
     * usuario);
     * 
     * return ResponseEntity.ok(envioActualizado);
     * } catch (RuntimeException e) {
     * return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
     * }
     * }
     * 
     * @PatchMapping("/{idEnvio}/estado")
     * 
     * @PreAuthorize("hasRole('CHOFER')")
     * public ResponseEntity<EstadoUpdateResponseDTO> actualizarEstado(
     * 
     * @PathVariable String idEnvio,
     * 
     * @RequestBody EstadoUpdateRequestDTO dto,
     * Authentication auth) {
     * 
     * // Extraemos el username del JWT
     * String username = auth.getName();
     * 
     * // Ejecutamos la lógica
     * Envio envioActualizado = envioService.actualizarEstadoChofer(idEnvio,
     * dto.getNuevoEstado(), username);
     * 
     * // Construimos la respuesta según el contrato
     * EstadoUpdateResponseDTO response = EstadoUpdateResponseDTO.builder()
     * .mensaje("Estado actualizado correctamente")
     * .estado_actual(envioActualizado.getEstado_actual().name())
     * .fecha_actualizacion(LocalDateTime.now())
     * .build();
     * 
     * return ResponseEntity.ok(response);
     * }
     */
    // endopitn cancelar envio
    // @PreAuthorize("hasAnyRole('OPERADOR', 'SUPERVISOR')") // Descomentar cuando
    // actives la seguridad por roles
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelarEnvio(@PathVariable String id, Principal principal) {
        try {
            // principal.getName() nos da el username del usuario logueado en el JWT
            Envio envioCancelado = envioService.cancelarEnvio(id, principal.getName());
            return ResponseEntity.ok(envioCancelado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // endopiont editar envio
    // @PreAuthorize("hasAnyRole('OPERADOR', 'SUPERVISOR')")
    @PutMapping("/{id}")
    public ResponseEntity<?> editarEnvio(@PathVariable String id, @RequestBody EnvioRequestDTO dto,
            Principal principal) {
        try {
            Envio envioEditado = envioService.editarEnvio(id, dto, principal.getName());
            return ResponseEntity.ok(envioEditado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    /////////////////////////////////////////////////////////////////////////////////////////////
}