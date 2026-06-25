package com.logitrack.sistema_logistica.controller;

import com.logitrack.sistema_logistica.dto.ReporteCumplimientoResponse;
import com.logitrack.sistema_logistica.dto.ReporteEficienciaDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.logitrack.sistema_logistica.service.ReporteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;
import com.logitrack.sistema_logistica.dto.ReporteEstadoDTO;
import org.springframework.web.bind.annotation.GetMapping;
import com.logitrack.sistema_logistica.dto.ReporteGranoDTO;
import com.logitrack.sistema_logistica.dto.ReporteSimpleDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.time.LocalDate;
import java.util.List;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    @Autowired
    private ReporteService reporteService;

    @GetMapping("/operativo")
    public ResponseEntity<?> reporteOperativo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        
        try {
            // Intentamos llamar al servicio
            ReporteSimpleDTO reporte = reporteService.obtenerReporte(fechaInicio, fechaFin);
            return ResponseEntity.ok(reporte);
            
        } catch (RuntimeException e) {

            // Si el servicio nos avisó que no hay datos con "EMPTY_STATE"
            if ("EMPTY_STATE".equals(e.getMessage())) {
                return ResponseEntity.noContent().build(); // Esto devuelve el 204
            }

            // Si es otro error, devolvemos un bad request
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/estados")
    public ResponseEntity<List<ReporteEstadoDTO>> obtenerReportePorEstados(
        @RequestParam(required = false) String rango) {
        return ResponseEntity.ok(reporteService.obtenerReportePorEstados(rango));
    }

    @GetMapping("/granos")
    public ResponseEntity<List<ReporteGranoDTO>> obtenerReportePorGrano(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return ResponseEntity.ok(reporteService.obtenerReportePorGrano(fechaInicio, fechaFin));
    }

    @GetMapping("/a-tiempo")
    public ResponseEntity<ReporteEficienciaDTO> obtenerEntregasATiempo(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
    
        // Ahora devuelve ReporteEficienciaDTO, no un Long
        return ResponseEntity.ok(reporteService.obtenerMetricasATiempo(fechaInicio, fechaFin));
    }
 
    @GetMapping("/cumplimiento")
    public ResponseEntity<ReporteCumplimientoResponse> getReporteCumplimiento(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
    
        return ResponseEntity.ok(reporteService.obtenerReporteCumplimiento(fechaInicio, fechaFin));
    }

    @GetMapping("/operativo/exportar/excel")
    public ResponseEntity<?> exportarOperativoExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        try {
            java.io.ByteArrayInputStream stream = reporteService.generarExcelOperativo(fechaInicio, fechaFin);
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=\"Logitrack_Operativo_" + LocalDate.now() + ".xlsx\"");
            headers.add("Access-Control-Expose-Headers", "Content-Disposition");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(new org.springframework.core.io.InputStreamResource(stream));

        } catch (RuntimeException | java.io.IOException e) {
            if ("EMPTY_STATE".equals(e.getMessage())) {
                return ResponseEntity.noContent().build(); // 204 No Content
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/cumplimiento/viajes/exportar/excel")
    public ResponseEntity<?> exportarCumplimientoExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        try {
            java.io.ByteArrayInputStream stream = reporteService.generarExcelCumplimiento(fechaInicio, fechaFin);
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=\"Logitrack_Cumplimiento_" + LocalDate.now() + ".xlsx\"");
            headers.add("Access-Control-Expose-Headers", "Content-Disposition");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(new org.springframework.core.io.InputStreamResource(stream));

        } catch (RuntimeException | java.io.IOException e) {
            if ("EMPTY_STATE".equals(e.getMessage())) {
                return ResponseEntity.noContent().build(); // 204 No Content
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/operativo/exportar")
    public void exportarReporteOperativoCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        
        try {
            response.setContentType("text/csv; charset=utf-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"Logitrack_Operativo_" + LocalDate.now() + ".csv\"");
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

            reporteService.exportarReporteOperativoCsv(fechaInicio, fechaFin, response.getWriter());

        } catch (RuntimeException e) {
            response.reset();
            if ("EMPTY_STATE".equals(e.getMessage())) {
                response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_NO_CONTENT); // 204 No Content
            } else {
                response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"message\": \"" + e.getMessage() + "\"}");
                response.getWriter().flush();
            }
        }
    }


    @GetMapping("/cumplimiento/viajes/exportar")
    public void exportarReporteCumplimientoCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        
        try {
            response.setContentType("text/csv; charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"Logitrack_Cumplimiento_" + LocalDate.now() + ".csv\"");
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

            reporteService.exportarViajesCumplimientoStreamCsv(fechaInicio, fechaFin, response.getWriter());
            
        } catch (RuntimeException e) {
            response.reset();
            if ("EMPTY_STATE".equals(e.getMessage())) {
                response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_NO_CONTENT); // 204 No Content
            } else {
                response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"message\": \"" + e.getMessage() + "\"}");
                response.getWriter().flush();
            }
        }
    }

    @GetMapping("/estadosPorFechas")
    public ResponseEntity<List<ReporteEstadoDTO>> obtenerReportePorEstadosPorFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return ResponseEntity.ok(reporteService.obtenerReportePorEstadosPorFechas(fechaInicio, fechaFin));
    }

    @GetMapping("/detalle/exportar")
    public void exportarDetalleCsv(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        
        try {
            response.setContentType("text/csv; charset=utf-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"Logitrack_DetalleEnvios_" + LocalDate.now() + ".csv\"");
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition"); // CLAVE PARA EL FRONTEND
            
            reporteService.exportarDetalleEnviosCsv(fechaInicio, fechaFin, response.getWriter());
            
        } catch (RuntimeException e) {
            response.reset();
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"message\": \"" + e.getMessage() + "\"}");
            response.getWriter().flush();
        }
    }

    @GetMapping("/detalle/exportar/excel")
    public ResponseEntity<?> exportarDetalleExcel(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        
        try {
            java.io.ByteArrayInputStream stream = reporteService.generarExcelDetalleEnvios(fechaInicio, fechaFin);
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=\"Logitrack_DetalleEnvios_" + LocalDate.now() + ".xlsx\"");
            headers.add("Access-Control-Expose-Headers", "Content-Disposition"); // CLAVE PARA EL FRONTEND
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(new org.springframework.core.io.InputStreamResource(stream));
                    
        } catch (RuntimeException | java.io.IOException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/cumplimiento/metricas/exportar")
    public ResponseEntity<byte[]> exportarCumplimientoCsvEndpoint(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        
        // Si no envían fechaInicio, usamos una fecha muy antigua (ej. 1 de Enero del 2000)
        LocalDateTime inicio = (fechaInicio != null) ? fechaInicio.atStartOfDay() : LocalDateTime.of(2000, 1, 1, 0, 0);
        
        // Si no envían fechaFin, usamos la fecha y hora actual para traer todo hasta el momento
        LocalDateTime fin = (fechaFin != null) ? fechaFin.atTime(23, 59, 59) : LocalDateTime.now();

        byte[] csvBytes = reporteService.exportarCumplimientoCsv(inicio, fin);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=cumplimiento_metricas.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvBytes);
    }

    @GetMapping("/cumplimiento/metricas/exportar/excel")
    public ResponseEntity<byte[]> exportarCumplimientoExcelEndpoint(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        
        LocalDateTime inicio = (fechaInicio != null) ? fechaInicio.atStartOfDay() : LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime fin = (fechaFin != null) ? fechaFin.atTime(23, 59, 59) : LocalDateTime.now();

        byte[] excelBytes = reporteService.exportarCumplimientoExcel(inicio, fin);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=cumplimiento_metricas.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }


}
