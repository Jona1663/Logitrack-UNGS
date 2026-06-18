package com.logitrack.sistema_logistica.controller;

import java.security.Principal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.logitrack.sistema_logistica.dto.AsignarTransporteDTO;
import com.logitrack.sistema_logistica.dto.CartaPorteDTO;
import com.logitrack.sistema_logistica.dto.EnvioDetalleResponseDTO;
import com.logitrack.sistema_logistica.dto.EnvioListadoDTO;
import com.logitrack.sistema_logistica.dto.EnvioOperativoDTO;
import com.logitrack.sistema_logistica.dto.EnvioRequestDTO;
import com.logitrack.sistema_logistica.dto.ErrorResponseDTO;
import com.logitrack.sistema_logistica.dto.HistorialResponseDTO;
import com.logitrack.sistema_logistica.dto.ReporteEficienciaDTO;
import com.logitrack.sistema_logistica.dto.ReporteGranoDTO;

import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.model.HistorialEstados;
import com.logitrack.sistema_logistica.model.Usuario;
import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;

import com.logitrack.sistema_logistica.repository.EnvioRepository;
import com.logitrack.sistema_logistica.repository.HistorialEstadosRepository;
import com.logitrack.sistema_logistica.repository.UsuarioRepository;
import com.logitrack.sistema_logistica.service.CartaPortePdfService;
import com.logitrack.sistema_logistica.service.CartaPorteService;
import com.logitrack.sistema_logistica.service.EnvioService;
import com.logitrack.sistema_logistica.service.ReporteService;

@RestController
@RequestMapping("/api/envios")
@RequiredArgsConstructor
public class EnvioController {

    @Autowired
    private EnvioService envioService;

    @Autowired
    private EnvioRepository envioRepository;


    private final CartaPorteService cartaPorteService;

    private final CartaPortePdfService cartaPortePdfService;

    @Autowired
    private UsuarioRepository usuarioRepository; // Inyectar repositorio -> Necesario para no enviar el ID de usuario
                                                 // desde el
                                                 // frontend, sino que el EnvioController extraiga quién es el usuario
                                                 // directamente leyendo el Token JWT de la petición. El usuario es
                                                 // necesario para auditorias.

    @Autowired
    private ReporteService reporteService;  // Inyectar el servicio de reportes para usar sus métodos en los endpoints de reportes
                                            // (ej: obtenerMetricasPorGrano, obtenerMetricasATiempo, etc.)
                                            // Estos métodos a su vez llaman a consultas


    /*
    //Versiion 1
    // GET para listar (siempre es útil tenerlo)
    @GetMapping
    public List<Envio> listarEnvios() {
        return envioRepository.findAll();
    }
    */

    //Version2
    // GET para listar (siempre es útil tenerlo)
    @GetMapping
    public ResponseEntity<List<EnvioListadoDTO>> listarEnvios() {
        List<Envio> enviosCrudos = envioRepository.findAll();
        
        List<EnvioListadoDTO> enviosParaFrontend = enviosCrudos.stream()
                .map(envio -> new EnvioListadoDTO(envio))
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(enviosParaFrontend);
    }

    // GET para buscar envíos con filtros opcionales por fecha, estado y paginación
    @GetMapping("/search")
    public ResponseEntity<?> buscarEnvios( @RequestParam(required = false) String query, @RequestParam(required = false) String estado, @RequestParam(required = false) String fecha, @RequestParam(required = false) String tipoGrano, @RequestParam(defaultValue = "0") int page,@RequestParam(defaultValue = "10") int size) {
        try {
            LocalDate fechaFiltro = null;
            EstadoEnvio estadoFiltro = null;

            // Parsear los parámetros
            if (fecha != null && !fecha.isBlank()) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                fechaFiltro = LocalDate.parse(fecha, formatter);
            }

            if (estado != null && !estado.isBlank()) {
                estadoFiltro = EstadoEnvio.valueOf(estado.toUpperCase());
            }

            LocalDateTime fechaInicio = null;
            LocalDateTime fechaFin = null;
            if (fechaFiltro != null) {
                fechaInicio = fechaFiltro.atStartOfDay();
                fechaFin = fechaFiltro.plusDays(1).atStartOfDay();
            }

            String termino = (query != null && !query.isBlank()) ? query.trim() : null;
            /* 
            Pageable pageable = PageRequest.of(page, size);
            Page<Envio> envios = envioService.buscarEnviosConFiltros(estadoFiltro, fechaInicio, fechaFin, termino,
                    tipoGrano,
                    pageable);
            return ResponseEntity.ok(envios);
            */

            Pageable pageable = PageRequest.of(page, size);
            Page<Envio> envios = envioService.buscarEnviosConFiltros(estadoFiltro, fechaInicio, fechaFin, termino,
                    tipoGrano,
                    pageable);
            
            // Transformamos la página de Envio a una página de EnvioListadoDTO usando .map()
            Page<EnvioListadoDTO> enviosDTO = envios.map(envio -> new EnvioListadoDTO(envio));
            
            return ResponseEntity.ok(enviosDTO);






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

    // Endpoint Global de Auditoría
    @Autowired
    private HistorialEstadosRepository historialEstadosRepository;

    @GetMapping("/historial-completo")
    public ResponseEntity<List<HistorialResponseDTO>> obtenerHistorialCompleto() {
        // Obtenemos todos los registros ordenados por fecha descendente (más reciente
        // primero)
        List<HistorialEstados> listaCompleta = historialEstadosRepository.findAll();

        // Mapeamos a nuestro DTO para enviar solo lo necesario y evitar errores de JSON
        List<HistorialResponseDTO> respuesta = listaCompleta.stream()
                .map(h -> {
                    HistorialResponseDTO dto = new HistorialResponseDTO();
                    dto.setIdHistorial(h.getIdHistorial());
                    dto.setIdEnvio(h.getEnvio() != null ? h.getEnvio().getIdEnvio() : null);
                    dto.setEstadoAnterior(h.getEstadoAnterior() != null ? h.getEstadoAnterior().name() : "INICIAL");
                    dto.setEstadoNuevo(h.getEstadoNuevo() != null ? h.getEstadoNuevo().name() : null);
                    dto.setFechaHora(h.getFechaHora());
                    dto.setUsername(h.getUsuario() != null ? h.getUsuario().getUsername() : null);
                    return dto;
                })
                // .sorted((a, b) -> b.getFechaHora().compareTo(a.getFechaHora())) // Ordenar
                // por fecha// ya no es necesario se ordena en el repository
                // consume menos memoria
                .collect(Collectors.toList());

        return ResponseEntity.ok(respuesta);
    }

    // Nuevo POST para crear envío. Necesario para no enviar el ID de usuario desde
    // el
    // frontend, sino que el EnvioController extraiga quién es el usuario
    // directamente leyendo el Token JWT de la petición. El ID de usuario es
    // necesario
    // para auditorias.





    //Original
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
            dto.setIdUsuarioCreador(usuario.getIdUsuario());

            Envio envioCreado = envioService.crearNuevoEnvio(dto);
            return new ResponseEntity<>(envioCreado, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    //Modificado para que el endpoint de creación de envíos no requiera autenticación, pero igual 
    // capture el usuario si el token JWT está presente. Esto es útil para permitir la creación de 
    // envíos desde sistemas externos que no manejen autenticación, pero aún así aprovechar la 
    // información del usuario cuando esté disponible para auditorías.
    // Se obtiene el envío completo esto habria que borrarlo a menos que se este
    // usando para algo
    @GetMapping("/buscar/{idEnvio}")
    public ResponseEntity<?> obtenerEnvioPorTracking(@PathVariable String idEnvio) {
        try {
            Envio envio = envioService.buscarPorId(idEnvio);
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

    ////////////////////////////////////////////////////////////////////////////////////////
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
            // #122 — Ahora devuelve el DTO con ETA calculado
            EnvioDetalleResponseDTO envio = envioService.obtenerDetalleConETA(idEnvio);
            return ResponseEntity.ok(envio);
        } catch (RuntimeException e) {
            ErrorResponseDTO error = new ErrorResponseDTO();
            error.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @PatchMapping("/{idEnvio}/estado")
    @PreAuthorize("hasRole('CHOFER')")
    public ResponseEntity<?> actualizarEstadoChofer( @PathVariable String idEnvio, @RequestParam String nuevoEstado,
            Authentication authentication) {
        try {
            // Extraemos el username del JWT
            String username = authentication.getName();

            // Ejecutamos la lógica
            Envio envioActualizado = envioService.actualizarEstadoChofer(
                    idEnvio, nuevoEstado, username);

            return ResponseEntity.ok(envioActualizado);
        } catch (RuntimeException e) {
            ErrorResponseDTO error = new ErrorResponseDTO();
            error.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // endopitn cancelar envio
    @PreAuthorize("hasRole('SUPERVISOR')") // Solo el supervisor puede cancelar un envío
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
    @PreAuthorize("hasAnyRole('OPERADOR', 'SUPERVISOR')")
    @PutMapping("/{id}")
    public ResponseEntity<?> editarEnvio(@PathVariable String id, @RequestBody EnvioRequestDTO dto, Principal principal) {
        try {
            Envio envioEditado = envioService.editarEnvio(id, dto, principal.getName());
            return ResponseEntity.ok(envioEditado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET — envíos sin chofer ni camión
    @GetMapping("/sin-asignar")
    public ResponseEntity<List<Envio>> listarSinAsignar() {
        return ResponseEntity.ok(envioRepository.findEnviosSinAsignar());
    }

    // PATCH — asignar chofer y camión juntos
    @PatchMapping("/{idEnvio}/asignar-transporte")
    public ResponseEntity<?> asignarTransporte( @PathVariable String idEnvio, @RequestBody AsignarTransporteDTO dto) {
        try {
            Envio envio = envioService.asignarTransporte(idEnvio, dto);
            return ResponseEntity.ok(envio);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // SOLUCIÓN TEMPORAL para editar los estados de un envío desde la vista de
    // operador/supervisor
    @PreAuthorize("hasAnyRole('OPERADOR', 'SUPERVISOR')")
    @PatchMapping("/{id}/operativo")
    public ResponseEntity<?> actualizarOperativaEnvio(@PathVariable String id, @RequestBody EnvioOperativoDTO dto,Authentication authentication) {
        try {
            Envio envioActualizado = envioService.actualizarEstadoOperativo(id, dto, authentication);
            return ResponseEntity.ok(envioActualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Endpoint para obtener la ubicación simulada del envío en tiempo real.

    @GetMapping("/{idEnvio}/tracking")
    @PreAuthorize("hasAnyRole('OPERADOR', 'SUPERVISOR', 'CHOFER')")
    public ResponseEntity<?> obtenerTrackingTiempoReal(@PathVariable String idEnvio) {
        try {
            Map<String, Object> trackingData = envioService.obtenerUbicacionActual(idEnvio);
            return ResponseEntity.ok(trackingData);
        } catch (RuntimeException e) {
            ErrorResponseDTO error = new ErrorResponseDTO();
            error.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // endpoint de la ruta que va a seguir el camion
    @GetMapping("/{idEnvio}/ruta-completa")
    @PreAuthorize("hasAnyRole('OPERADOR', 'SUPERVISOR', 'CHOFER')")
    public ResponseEntity<?> obtenerRutaCompleta(@PathVariable String idEnvio) {
        try {
            JsonNode rutaJson = envioService.obtenerGeometriaRuta(idEnvio);
        
            ObjectMapper mapper = new ObjectMapper();
            // ✅ Convertir JsonNode a Object "puro" (List, Map, etc.)
            Object coordinates = mapper.convertValue(rutaJson, Object.class);
        
            return ResponseEntity.ok(Map.of(
                    "idEnvio", idEnvio,
                    "coordinates", coordinates));
        } catch (RuntimeException e) {
            ErrorResponseDTO error = new ErrorResponseDTO();
            error.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @GetMapping("/reportes/granos")
    public ResponseEntity<List<ReporteGranoDTO>> getReporteGranos( @RequestParam LocalDateTime fechaInicio, @RequestParam LocalDateTime fechaFin) {
        return ResponseEntity.ok(envioRepository.obtenerMetricasPorGrano(fechaInicio, fechaFin));
    }

    @GetMapping("/reportes/a-tiempo")
    public ResponseEntity<ReporteEficienciaDTO> getReporteATiempo( @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        // Llamamos al SERVICIO, no al repositorio
        return ResponseEntity.ok(reporteService.obtenerMetricasATiempo(fechaInicio, fechaFin));
    }

    //PARA LA TAREA #460
    @GetMapping("/{id}/carta-porte")
    public ResponseEntity<CartaPorteDTO> obtenerCartaPorteQR(@PathVariable String id) {
        CartaPorteDTO cartaPorte = cartaPorteService.obtenerCartaPorte(id);
        return ResponseEntity.ok(cartaPorte);
    }

    // Endpoint para descargar la Carta de Porte en formato PDF
    @GetMapping("/{id}/pdf-carta-porte")
    public ResponseEntity<byte[]> descargarPdfCartaPorte(@PathVariable String id) {
        
        // 1. Llamamos al servicio para que nos de los bytes del PDF
        byte[] pdfBytes = cartaPortePdfService.generarPdf(id);

        // 2. Configuramos las cabeceras HTTP para indicarle al navegador que esto es un archivo descargable
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        // attachment; filename="..." fuerza al navegador a descargarlo como archivo con ese nombre
        headers.setContentDispositionFormData("attachment", "Carta_Porte_" + id + ".pdf");

        // 3. Retornamos el archivo
        return org.springframework.http.ResponseEntity
                .ok()
                .headers(headers)
                .body(pdfBytes);
    }
    
}