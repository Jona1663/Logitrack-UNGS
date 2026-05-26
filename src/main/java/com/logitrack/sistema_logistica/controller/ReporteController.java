package com.logitrack.sistema_logistica.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.logitrack.sistema_logistica.dto.ReporteCumplimientoResponse;
import com.logitrack.sistema_logistica.dto.ReporteEficienciaDTO;
import com.logitrack.sistema_logistica.dto.ReporteEstadoDTO;
import com.logitrack.sistema_logistica.dto.ReporteGranoDTO;
import com.logitrack.sistema_logistica.dto.ReporteSimpleDTO;
import com.logitrack.sistema_logistica.service.ReporteService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;



@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    @Autowired
    private ReporteService reporteService;

    // GET /api/reportes/operativo?fechaInicio=...&fechaFin=...
    // Criterio 1: Reporte Operativo (total de viajes y kilos)
    // Si el usuario no envía fechas, se muestra el histórico completo
    // Si el usuario envía fechas, se muestran solo los datos de ese rango
    // Ejemplo: GET /api/reportes/operativo?fechaInicio=2024-01-01&fechaFin=2024-01-31
    // Ejemplo sin fechas: GET /api/reportes/operativo
    // El formato de fecha es ISO (YYYY-MM-DD)
    // El rango de fechas es opcional, pero si se envía, ambos parámetros deben estar presentes
    @GetMapping("/operativo")
    public ResponseEntity<ReporteSimpleDTO> reporteOperativo(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        ReporteSimpleDTO reporte = reporteService.obtenerReporte(fechaInicio, fechaFin);
        return ResponseEntity.ok(reporte);
    }

    //Desglose por estados (Criterio 2)
    // GET /api/reportes/estados?rango=ultimos7dias
    // GET /api/reportes/estados?rango=ultimos30dias
    // GET /api/reportes/estados?rango=ultimos90dias
    // GET /api/reportes/estados (sin rango, muestra todo el histórico)
    // El formato del rango es "ultimosXdias", donde X es un número entero
    @GetMapping("/estados")
    public ResponseEntity<List<ReporteEstadoDTO>> obtenerReportePorEstados(
        @RequestParam(required = false) String rango) {
        return ResponseEntity.ok(reporteService.obtenerReportePorEstados(rango));
    }

    // GET /api/reportes/granos?fechaInicio=...&fechaFin=...
    // Criterio 3: Desglose por tipo de grano
    // El formato de fecha es ISO (YYYY-MM-DD)
    // El rango de fechas es obligatorio para este reporte
    // Ejemplo: GET /api/reportes/granos?fechaInicio=2024-01-01&fechaFin=2024-01-31    
    // Si el usuario no envía fechas, se muestra un error indicando que las fechas son obligatorias
    @GetMapping("/granos")
    public ResponseEntity<List<ReporteGranoDTO>> obtenerReportePorGrano(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return ResponseEntity.ok(reporteService.obtenerReportePorGrano(fechaInicio, fechaFin));
    }

    // GET /api/reportes/a-tiempo?fechaInicio=...&fechaFin=...
    // Criterio 4: Envíos que llegaron a tiempo
    // El formato de fecha es ISO (YYYY-MM-DD)
    // El rango de fechas es obligatorio para este reporte
    // Ejemplo: GET /api/reportes/a-tiempo?fechaInicio=2024-01-01&fechaFin=2024-01-31
    // Si el usuario no envía fechas, se muestra un error indicando que las fechas son obligatorias
    // La respuesta es un ReporteEficienciaDTO que contiene la cantidad de envíos a tiempo y los kilos correspondientes
    // El cálculo de "a tiempo" se basa en comparar la fecha de llegada real con la fecha estimada de llegada
    // Un envío se considera "a tiempo" si su fecha de llegada real es igual o anterior a su fecha estimada de llegada
    @GetMapping("/a-tiempo")
    public ResponseEntity<ReporteEficienciaDTO> obtenerEntregasATiempo(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
    
        // Ahora devuelve ReporteEficienciaDTO, no un Long
        return ResponseEntity.ok(reporteService.obtenerMetricasATiempo(fechaInicio, fechaFin));
    }

    //237 exponemos la ruta GET /api/v1/cumplimiento que devuelve un ReporteCumplimientoResponse 
    // con las métricas de cumplimiento y la lista de viajes para el período indicado.
    // El ReporteCumplimientoResponse incluye:
    // - porcentajeCumplimiento: el porcentaje de viajes que llegaron a tiempo (entrega real <= ETA)
    // - desvioPromedioHoras: el desvío promedio en horas entre la fecha de entrega real y la fecha estimada de llegada (ETA)
    // - viajes: una lista de ViajeCumplimientoDTO que contiene el detalle de cada viaje, incluyendo el cálculo del desvío en horas y si fue retrasado o no.
    // El cálculo del porcentaje de cumplimiento se hace dividiendo la cantidad de viajes a tiempo por el total de viajes completados en el período, multiplicado por 100.  
    // El cálculo del desvío promedio en horas se hace sumando el desvío en horas de cada viaje (positivo si llegó después del ETA, negativo si llegó antes) y dividiendo por la cantidad total de viajes completados.  
    @GetMapping("/v1/cumplimiento")
    public ResponseEntity<ReporteCumplimientoResponse> getReporteCumplimiento(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
    
        return ResponseEntity.ok(reporteService.obtenerReporteCumplimiento(fechaInicio, fechaFin));
    }

    /*
    // Endpoint A: Exportación Operativa
    //Opcion 1
    @GetMapping("/operativo/exportar")
    public void exportarReporteOperativoCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        try {
            response.setContentType("text/csv; charset=UTF-8");
            response.setHeader(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Logitrack_Operativo_" + LocalDate.now() + ".csv\"");
            response.setHeader(org.springframework.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, org.springframework.http.HttpHeaders.CONTENT_DISPOSITION);
            
            reporteService.exportarReporteOperativoCsv(fechaInicio, fechaFin, response.getWriter());
        } catch (RuntimeException e) {
            response.setStatus(400);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"message\": \"" + e.getMessage() + "\"}");
        }
    }
    */
    //Opcion 2

    @GetMapping("/operativo/exportar")
    public void exportarReporteOperativoCsv(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        
        try {
            // --- CUMPLIENDO EL PUNTO 1: Configurar cabeceras de descarga ---
            response.setContentType("text/csv; charset=utf-8");
            String fechaHoy = java.time.LocalDate.now().toString();
            response.setHeader("Content-Disposition", "attachment; filename=\"Logitrack_ReporteOperativo_" + fechaHoy + ".csv\"");
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

            // Llamamos al servicio pasando el writer de la respuesta HTTP
            reporteService.exportarReporteOperativoCsv(fechaInicio, fechaFin, response.getWriter());

        } catch (RuntimeException e) {
            // --- CUMPLIENDO EL PUNTO 4: Si salta el error de datos vacíos, limpiamos la respuesta y mandamos JSON con Error 400 ---
            response.reset();
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"message\": \"" + e.getMessage() + "\"}");
            response.getWriter().flush();
        }
    }











    // Endpoint B: Exportación de Cumplimiento
    @GetMapping("/cumplimiento/viajes/exportar")
    public void exportarReporteCumplimientoCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        try {
            response.setContentType("text/csv; charset=UTF-8");
            response.setHeader(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Logitrack_Cumplimiento_" + LocalDate.now() + ".csv\"");
            response.setHeader(org.springframework.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, org.springframework.http.HttpHeaders.CONTENT_DISPOSITION);

            reporteService.exportarViajesCumplimientoStreamCsv(fechaInicio, fechaFin, response.getWriter());
        } catch (RuntimeException e) {
            response.setStatus(400);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"message\": \"" + e.getMessage() + "\"}");
        }
    }



}
