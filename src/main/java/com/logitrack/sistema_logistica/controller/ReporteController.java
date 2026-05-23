package com.logitrack.sistema_logistica.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logitrack.sistema_logistica.dto.ReporteSimpleDTO;
import com.logitrack.sistema_logistica.service.ReporteService;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    @Autowired
    private ReporteService reporteService;

    @GetMapping("/operativo")
    public ResponseEntity<ReporteSimpleDTO> reporteOperativo() {
        ReporteSimpleDTO reporte = reporteService.obtenerReporte();
        return ResponseEntity.ok(reporte);
    }
}
