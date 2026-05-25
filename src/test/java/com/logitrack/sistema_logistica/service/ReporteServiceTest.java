package com.logitrack.sistema_logistica.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;
import com.logitrack.sistema_logistica.repository.EnvioRepository;
import com.logitrack.sistema_logistica.dto.ReporteEstadoDTO;
import com.logitrack.sistema_logistica.dto.ReporteCumplimientoResponse;

@ExtendWith(MockitoExtension.class)
public class ReporteServiceTest {

    @InjectMocks
    private ReporteService reporteService;

    @Mock
    private EnvioRepository envioRepository;

    // =========================================================
    // (CSV CON COMAS) 
    // =========================================================
    @Test
    public void testExportarViajes_ConComas_NoRompeColumnas() throws IOException {
        // Arrange
        LocalDate fechaInicio = LocalDate.now().minusDays(5);
        LocalDate fechaFin = LocalDate.now();
        LocalDateTime inicioDateTime = fechaInicio.atStartOfDay();
        LocalDateTime finDateTime = fechaFin.atTime(23, 59, 59);

        // Simulamos que hay registros para que no salte la excepción
        when(envioRepository.countEntreFechas(inicioDateTime, finDateTime)).thenReturn(1L);

        // Mandamos el ID con coma para ver si sobrevive al CSV
        Envio envioMock = Envio.builder()
                .idEnvio("Rosario, Santa Fe")
                .estadoActual(EstadoEnvio.ENTREGADO)
                .fechaEstimadaLlegada(LocalDateTime.now())
                .fechaLlegada(LocalDateTime.now().plusHours(2)) // Genera retraso a propósito
                .build();

        when(envioRepository.obtenerEnviosComoStreamParaExportacion(inicioDateTime, finDateTime))
                .thenReturn(Stream.of(envioMock));

        StringWriter stringWriter = new StringWriter();

        // Act
        reporteService.exportarViajesCumplimientoStreamCsv(fechaInicio, fechaFin, stringWriter);

        // Assert
        String csvResult = stringWriter.toString();
        assertNotNull(csvResult);
        
        // Como el delimitador es ';', Apache no le pone comillas extras a la coma.
        // Validamos que el texto esté tal cual y que las columnas no se rompieron.
        assertTrue(csvResult.contains("Rosario, Santa Fe"));
        assertTrue(csvResult.contains("ENTREGADO"));
        assertTrue(csvResult.contains("h de retraso")); 
    }

    // =========================================================
    // 2. (Reporte Operativo)
    // =========================================================
    @Test
    public void testExportarReporteOperativoCsv_SubeCoberturaJacoco() throws IOException {
        // Arrange
        LocalDate fechaInicio = LocalDate.now().minusDays(5);
        LocalDate fechaFin = LocalDate.now();
        LocalDateTime inicioDateTime = fechaInicio.atStartOfDay();
        LocalDateTime finDateTime = fechaFin.atTime(23, 59, 59);

        when(envioRepository.countEntreFechas(inicioDateTime, finDateTime)).thenReturn(10L);
        when(envioRepository.sumKilosEntreFechas(inicioDateTime, finDateTime)).thenReturn(5000L);

        // Usamos un mock del DTO para evitar errores si no tiene constructores
        ReporteEstadoDTO estadoDTO = mock(ReporteEstadoDTO.class);
        when(estadoDTO.getEstado()).thenReturn("PENDIENTE");
        when(estadoDTO.getCantidadEnvios()).thenReturn(10L);

        when(envioRepository.obtenerMetricasPorEstadoEntreFechas(inicioDateTime, finDateTime))
                .thenReturn(List.of(estadoDTO));

        StringWriter stringWriter = new StringWriter();

        // Act
        reporteService.exportarReporteOperativoCsv(fechaInicio, fechaFin, stringWriter);

        // Assert
        String csvResult = stringWriter.toString();
        assertTrue(csvResult.contains("Total de Viajes;10"));
        assertTrue(csvResult.contains("Kilos Transportados (kg);5000"));
        assertTrue(csvResult.contains("Estado: PENDIENTE;10"));
    }
    
    // =========================================================
    // (Cumplimiento)
    // =========================================================
    @Test
    public void testObtenerReporteCumplimiento_SubeCoberturaJacoco() {
        // Arrange
        LocalDate fechaInicio = LocalDate.now().minusDays(5);
        LocalDate fechaFin = LocalDate.now();
        LocalDateTime inicioDateTime = fechaInicio.atStartOfDay();
        LocalDateTime finDateTime = fechaFin.atTime(23, 59, 59);

        Envio envio1 = Envio.builder()
                .idEnvio("LT-1")
                .estadoActual(EstadoEnvio.ENTREGADO)
                .fechaEstimadaLlegada(LocalDateTime.now())
                .fechaLlegada(LocalDateTime.now().minusHours(1)) // A tiempo
                .build();

        when(envioRepository.obtenerEnviosCompletadosParaCumplimiento(inicioDateTime, finDateTime))
                .thenReturn(List.of(envio1));

        // Act
        ReporteCumplimientoResponse reporte = reporteService.obtenerReporteCumplimiento(fechaInicio, fechaFin);

        // Assert
        assertNotNull(reporte);
        assertEquals(1, reporte.getMetricas().getTotalEntregados());
        assertEquals(1, reporte.getMetricas().getEntregadosATiempo());
    }
}