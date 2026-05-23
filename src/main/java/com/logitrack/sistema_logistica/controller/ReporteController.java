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

import com.logitrack.sistema_logistica.dto.ReporteEstadoDTO;
import com.logitrack.sistema_logistica.dto.ReporteSimpleDTO;
import com.logitrack.sistema_logistica.service.ReporteService;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    @Autowired
    private ReporteService reporteService;

    @GetMapping("/operativo")
    public ResponseEntity<ReporteSimpleDTO> reporteOperativo(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        ReporteSimpleDTO reporte = reporteService.obtenerReporte(fechaInicio, fechaFin);
        return ResponseEntity.ok(reporte);
    }

    //Desglose por estados (Criterio 2)
    @GetMapping("/estados")
    public ResponseEntity<List<ReporteEstadoDTO>> obtenerReportePorEstados(
        @RequestParam(required = false) String rango) {
        return ResponseEntity.ok(reporteService.obtenerReportePorEstados(rango));
    }

    // GET /api/reportes/granos?fechaInicio=...&fechaFin=...
    @GetMapping("/granos")
    public ResponseEntity<List<ReporteEstadoDTO>> obtenerReportePorGrano(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return ResponseEntity.ok(reporteService.obtenerReportePorGrano(fechaInicio, fechaFin));
    }

    // GET /api/reportes/a-tiempo?fechaInicio=...&fechaFin=...
    @GetMapping("/a-tiempo")
    public ResponseEntity<Long> obtenerEntregasATiempo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return ResponseEntity.ok(reporteService.obtenerCantidadATiempo(fechaInicio, fechaFin));
    }





}
