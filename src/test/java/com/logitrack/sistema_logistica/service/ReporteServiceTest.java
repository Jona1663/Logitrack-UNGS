package com.logitrack.sistema_logistica.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
import com.logitrack.sistema_logistica.dto.ReporteSimpleDTO;
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

    // =========================================================
    // TICKET #236: Pruebas de cálculo de Reporte Operativo (Volumen y Viajes)
    // =========================================================

    @Test
    public void obtenerReporte_ConFechas_SumaKilosYCuentaBien() {
        // Arrange: Simulamos un filtro de fechas
        LocalDate inicio = LocalDate.of(2026, 1, 1);
        LocalDate fin = LocalDate.of(2026, 1, 31);
        LocalDateTime inicioDt = inicio.atStartOfDay();
        LocalDateTime finDt = fin.atTime(23, 59, 59);

        // Simulamos que la base de datos nos dice que hubo 15 viajes y pesaron 25.000 kg
        when(envioRepository.countEntreFechas(inicioDt, finDt)).thenReturn(15L);
        when(envioRepository.sumKilosEntreFechas(inicioDt, finDt)).thenReturn(25000L);

        // Act: Ejecutamos el método
        ReporteSimpleDTO resultado = reporteService.obtenerReporte(inicio, fin);

        // Assert: Validamos que los cálculos sean correctos
        assertNotNull(resultado);
        assertEquals(15L, resultado.getTotalViajes());
        assertEquals(25000L, resultado.getTotalKilos());
    }

    @Test
    public void obtenerReporte_SinFechas_TraeHistoricoCompleto() {
        // Arrange: Simulamos que no mandaron fechas (piden todo el histórico)
        when(envioRepository.count()).thenReturn(50L);
        when(envioRepository.sumKilos()).thenReturn(100000L);

        // Act: Le pasamos null a las fechas
        ReporteSimpleDTO resultado = reporteService.obtenerReporte(null, null);

        // Assert
        assertNotNull(resultado);
        assertEquals(50L, resultado.getTotalViajes());
        assertEquals(100000L, resultado.getTotalKilos());
    }

    @Test
    public void obtenerReportePorEstados_Ultimos7Dias_CuentaEstados() {
        // Arrange: Creamos estados simulados
        ReporteEstadoDTO estadoPendiente = mock(ReporteEstadoDTO.class);
        when(estadoPendiente.getEstado()).thenReturn("PENDIENTE");
        when(estadoPendiente.getCantidadEnvios()).thenReturn(10L);

        ReporteEstadoDTO estadoEntregado = mock(ReporteEstadoDTO.class);
        when(estadoEntregado.getEstado()).thenReturn("ENTREGADO");
        when(estadoEntregado.getCantidadEnvios()).thenReturn(25L);

        // OJO ACÁ: Como el código usa "LocalDateTime.now()" por dentro, los milisegundos 
        // nunca van a coincidir exactos. Por eso usamos "any(LocalDateTime.class)" para atajarlo.
        when(envioRepository.obtenerMetricasPorEstadoEntreFechas(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(estadoPendiente, estadoEntregado));

        // Act
        List<ReporteEstadoDTO> resultados = reporteService.obtenerReportePorEstados("ULTIMOS_7_DIAS");

        // Assert
        assertEquals(2, resultados.size());
        assertEquals("PENDIENTE", resultados.get(0).getEstado());
        assertEquals(10L, resultados.get(0).getCantidadEnvios());
        assertEquals("ENTREGADO", resultados.get(1).getEstado());
        assertEquals(25L, resultados.get(1).getCantidadEnvios());
    }

    @Test
    public void obtenerReportePorEstados_Historico_CuentaEstados() {
        // Arrange: Creamos un estado simulado para el histórico general
        ReporteEstadoDTO estadoMock = mock(ReporteEstadoDTO.class);
        when(estadoMock.getEstado()).thenReturn("EN_TRANSITO");
        when(estadoMock.getCantidadEnvios()).thenReturn(5L);

        when(envioRepository.obtenerMetricasPorEstado()).thenReturn(List.of(estadoMock));

        // Act: Le pasamos cualquier cosa que no sea "ULTIMOS_7_DIAS" (o null)
        List<ReporteEstadoDTO> resultados = reporteService.obtenerReportePorEstados("HISTORICO_COMPLETO");

        // Assert
        assertEquals(1, resultados.size());
        assertEquals("EN_TRANSITO", resultados.get(0).getEstado());
        assertEquals(5L, resultados.get(0).getCantidadEnvios());
    }
}