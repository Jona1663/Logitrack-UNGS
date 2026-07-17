package com.logitrack.sistema_logistica.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.logitrack.sistema_logistica.dto.ReporteCumplimientoResponse;
import com.logitrack.sistema_logistica.dto.ReporteEficienciaDTO;
import com.logitrack.sistema_logistica.dto.ReporteSimpleDTO;
import com.logitrack.sistema_logistica.service.ReporteService;

@ExtendWith(MockitoExtension.class)
public class ReporteControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ReporteService reporteService;

    @InjectMocks
    private ReporteController reporteController;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(reporteController).build();
    }

    // ==========================================
    // 1. REPORTE OPERATIVO (JSON)
    // ==========================================
    @Test
    void reporteOperativo_CaminoFeliz() throws Exception {
        when(reporteService.obtenerReporte(any(), any())).thenReturn(new ReporteSimpleDTO());

        mockMvc.perform(get("/api/reportes/operativo"))
               .andExpect(status().isOk());
    }

    @Test
    void reporteOperativo_EmptyState() throws Exception {
        when(reporteService.obtenerReporte(any(), any())).thenThrow(new RuntimeException("EMPTY_STATE"));

        mockMvc.perform(get("/api/reportes/operativo"))
               .andExpect(status().isNoContent()); // 204
    }

    @Test
    void reporteOperativo_Error() throws Exception {
        when(reporteService.obtenerReporte(any(), any())).thenThrow(new RuntimeException("Fallo en la BD"));

        mockMvc.perform(get("/api/reportes/operativo"))
               .andExpect(status().isBadRequest()); // 400
    }

    // ==========================================
    // 2. REPORTE POR ESTADOS
    // ==========================================
    @Test
    void obtenerReportePorEstados_CaminoFeliz() throws Exception {
        when(reporteService.obtenerReportePorEstados(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/reportes/estados?rango=ultimos7dias"))
               .andExpect(status().isOk());
    }

    // ==========================================
    // 3. REPORTE POR GRANOS
    // ==========================================
    @Test
    void obtenerReportePorGrano_CaminoFeliz() throws Exception {
        when(reporteService.obtenerReportePorGrano(any(LocalDate.class), any(LocalDate.class)))
               .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/reportes/granos")
               .param("fechaInicio", "2024-01-01")
               .param("fechaFin", "2024-01-31"))
               .andExpect(status().isOk());
    }

    // ==========================================
    // 4. ENTREGAS A TIEMPO
    // ==========================================
    @Test
    void obtenerEntregasATiempo_CaminoFeliz() throws Exception {
        when(reporteService.obtenerMetricasATiempo(any(LocalDate.class), any(LocalDate.class)))
               .thenReturn(new ReporteEficienciaDTO());

        mockMvc.perform(get("/api/reportes/a-tiempo")
               .param("fechaInicio", "2024-01-01")
               .param("fechaFin", "2024-01-31"))
               .andExpect(status().isOk());
    }

    // ==========================================
    // 5. CUMPLIMIENTO GLOBAL
    // ==========================================
    @Test
    void getReporteCumplimiento_CaminoFeliz() throws Exception {
        when(reporteService.obtenerReporteCumplimiento(any(LocalDate.class), any(LocalDate.class)))
               .thenReturn(new ReporteCumplimientoResponse());

        mockMvc.perform(get("/api/reportes/cumplimiento")
               .param("fechaInicio", "2024-01-01")
               .param("fechaFin", "2024-01-31"))
               .andExpect(status().isOk());
    }

    // ==========================================
    // 6. EXPORTAR OPERATIVO (EXCEL)
    // ==========================================
    @Test
    void exportarOperativoExcel_CaminoFeliz() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(new byte[0]);
        when(reporteService.generarExcelOperativo(any(), any())).thenReturn(bais);

        mockMvc.perform(get("/api/reportes/operativo/exportar/excel"))
               .andExpect(status().isOk());
    }

    @Test
    void exportarOperativoExcel_EmptyState() throws Exception {
        when(reporteService.generarExcelOperativo(any(), any())).thenThrow(new RuntimeException("EMPTY_STATE"));

        mockMvc.perform(get("/api/reportes/operativo/exportar/excel"))
               .andExpect(status().isNoContent()); // 204
    }

    @Test
    void exportarOperativoExcel_Error() throws Exception {
        when(reporteService.generarExcelOperativo(any(), any())).thenThrow(new RuntimeException("Error simulado"));

        mockMvc.perform(get("/api/reportes/operativo/exportar/excel"))
               .andExpect(status().isBadRequest()); // 400
    }

    // ==========================================
    // 7. EXPORTAR CUMPLIMIENTO VIAJES (EXCEL)
    // ==========================================
    @Test
    void exportarCumplimientoExcel_CaminoFeliz() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(new byte[0]);
        when(reporteService.generarExcelCumplimiento(any(LocalDate.class), any(LocalDate.class))).thenReturn(bais);

        mockMvc.perform(get("/api/reportes/cumplimiento/viajes/exportar/excel")
               .param("fechaInicio", "2024-01-01")
               .param("fechaFin", "2024-01-31"))
               .andExpect(status().isOk());
    }

    @Test
    void exportarCumplimientoExcel_EmptyState() throws Exception {
        when(reporteService.generarExcelCumplimiento(any(LocalDate.class), any(LocalDate.class)))
               .thenThrow(new RuntimeException("EMPTY_STATE"));

        mockMvc.perform(get("/api/reportes/cumplimiento/viajes/exportar/excel")
               .param("fechaInicio", "2024-01-01")
               .param("fechaFin", "2024-01-31"))
               .andExpect(status().isNoContent());
    }

    @Test
    void exportarCumplimientoExcel_Error() throws Exception {
        when(reporteService.generarExcelCumplimiento(any(LocalDate.class), any(LocalDate.class)))
               .thenThrow(new RuntimeException("Error simulado"));

        mockMvc.perform(get("/api/reportes/cumplimiento/viajes/exportar/excel")
               .param("fechaInicio", "2024-01-01")
               .param("fechaFin", "2024-01-31"))
               .andExpect(status().isBadRequest());
    }

    // ==========================================
    // 8. EXPORTAR OPERATIVO (CSV)
    // ==========================================
    @Test
    void exportarReporteOperativoCsv_CaminoFeliz() throws Exception {
        mockMvc.perform(get("/api/reportes/operativo/exportar"))
               .andExpect(status().isOk());
    }

    @Test
    void exportarReporteOperativoCsv_EmptyState() throws Exception {
        doThrow(new RuntimeException("EMPTY_STATE")).when(reporteService).exportarReporteOperativoCsv(any(), any(), any());

        mockMvc.perform(get("/api/reportes/operativo/exportar"))
               .andExpect(status().isNoContent());
    }

    @Test
    void exportarReporteOperativoCsv_Error() throws Exception {
        doThrow(new RuntimeException("Fallo generar CSV")).when(reporteService).exportarReporteOperativoCsv(any(), any(), any());

        mockMvc.perform(get("/api/reportes/operativo/exportar"))
               .andExpect(status().isBadRequest());
    }

    // ==========================================
    // 9. EXPORTAR CUMPLIMIENTO VIAJES (CSV)
    // ==========================================
    @Test
    void exportarReporteCumplimientoCsv_CaminoFeliz() throws Exception {
        mockMvc.perform(get("/api/reportes/cumplimiento/viajes/exportar")
               .param("fechaInicio", "2024-01-01")
               .param("fechaFin", "2024-01-31"))
               .andExpect(status().isOk());
    }

    @Test
    void exportarReporteCumplimientoCsv_EmptyState() throws Exception {
        doThrow(new RuntimeException("EMPTY_STATE")).when(reporteService).exportarViajesCumplimientoStreamCsv(any(), any(), any());

        mockMvc.perform(get("/api/reportes/cumplimiento/viajes/exportar")
               .param("fechaInicio", "2024-01-01")
               .param("fechaFin", "2024-01-31"))
               .andExpect(status().isNoContent());
    }

    @Test
    void exportarReporteCumplimientoCsv_Error() throws Exception {
        doThrow(new RuntimeException("Error")).when(reporteService).exportarViajesCumplimientoStreamCsv(any(), any(), any());

        mockMvc.perform(get("/api/reportes/cumplimiento/viajes/exportar")
               .param("fechaInicio", "2024-01-01")
               .param("fechaFin", "2024-01-31"))
               .andExpect(status().isBadRequest());
    }

    // ==========================================
    // 10. ESTADOS POR FECHAS
    // ==========================================
    @Test
    void obtenerReportePorEstadosPorFechas_CaminoFeliz() throws Exception {
        when(reporteService.obtenerReportePorEstadosPorFechas(any(LocalDate.class), any(LocalDate.class)))
               .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/reportes/estadosPorFechas")
               .param("fechaInicio", "2024-01-01")
               .param("fechaFin", "2024-01-31"))
               .andExpect(status().isOk());
    }

    // ==========================================
    // 11. DETALLE EXPORTAR (CSV)
    // ==========================================
    @Test
    void exportarDetalleCsv_CaminoFeliz() throws Exception {
        mockMvc.perform(get("/api/reportes/detalle/exportar"))
               .andExpect(status().isOk());
    }

    @Test
    void exportarDetalleCsv_Error() throws Exception {
        doThrow(new RuntimeException("Fallo detalle CSV")).when(reporteService).exportarDetalleEnviosCsv(any(), any(), any());

        mockMvc.perform(get("/api/reportes/detalle/exportar"))
               .andExpect(status().isBadRequest());
    }

    // ==========================================
    // 12. DETALLE EXPORTAR (EXCEL)
    // ==========================================
    @Test
    void exportarDetalleExcel_CaminoFeliz() throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(new byte[0]);
        when(reporteService.generarExcelDetalleEnvios(any(), any())).thenReturn(bais);

        mockMvc.perform(get("/api/reportes/detalle/exportar/excel"))
               .andExpect(status().isOk());
    }

    @Test
    void exportarDetalleExcel_Error() throws Exception {
        when(reporteService.generarExcelDetalleEnvios(any(), any())).thenThrow(new RuntimeException("Error Excel Detalle"));

        mockMvc.perform(get("/api/reportes/detalle/exportar/excel"))
               .andExpect(status().isBadRequest());
    }

    // ==========================================
    // 13. CUMPLIMIENTO METRICAS EXPORTAR (CSV)
    // ==========================================
    @Test
    void exportarCumplimientoMetricasCsv_SinFechas() throws Exception {
        when(reporteService.exportarCumplimientoCsv(any(LocalDateTime.class), any(LocalDateTime.class)))
               .thenReturn(new byte[0]);

        mockMvc.perform(get("/api/reportes/cumplimiento/metricas/exportar"))
               .andExpect(status().isOk());
    }

    @Test
    void exportarCumplimientoMetricasCsv_ConFechas() throws Exception {
        when(reporteService.exportarCumplimientoCsv(any(LocalDateTime.class), any(LocalDateTime.class)))
               .thenReturn(new byte[0]);

        mockMvc.perform(get("/api/reportes/cumplimiento/metricas/exportar")
               .param("fechaInicio", "2024-01-01")
               .param("fechaFin", "2024-01-31"))
               .andExpect(status().isOk());
    }

    // ==========================================
    // 14. CUMPLIMIENTO METRICAS EXPORTAR (EXCEL)
    // ==========================================
    @Test
    void exportarCumplimientoMetricasExcel_SinFechas() throws Exception {
        when(reporteService.exportarCumplimientoExcel(any(LocalDateTime.class), any(LocalDateTime.class)))
               .thenReturn(new byte[0]);

        mockMvc.perform(get("/api/reportes/cumplimiento/metricas/exportar/excel"))
               .andExpect(status().isOk());
    }

    @Test
    void exportarCumplimientoMetricasExcel_ConFechas() throws Exception {
        when(reporteService.exportarCumplimientoExcel(any(LocalDateTime.class), any(LocalDateTime.class)))
               .thenReturn(new byte[0]);

        mockMvc.perform(get("/api/reportes/cumplimiento/metricas/exportar/excel")
               .param("fechaInicio", "2024-01-01")
               .param("fechaFin", "2024-01-31"))
               .andExpect(status().isOk());
    }
    // =========================================================
    // NUEVOS TESTS PARA TICKET #365: Validación de Parámetros
    // =========================================================

    @Test
    void reporteOperativo_SinFechas_RetornaOk() throws Exception {
        when(reporteService.obtenerReporte(null, null)).thenReturn(new ReporteSimpleDTO());

        mockMvc.perform(get("/api/reportes/operativo"))
               .andExpect(status().isOk());
    }

    @Test
    void exportarOperativoExcel_ConFechas_RetornaOk() throws Exception {
        // Simulamos fechas para el export
        when(reporteService.generarExcelOperativo(any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(new ByteArrayInputStream(new byte[0]));

        mockMvc.perform(get("/api/reportes/operativo/exportar/excel")
                .param("fechaInicio", "2024-01-01")
                .param("fechaFin", "2024-01-31"))
               .andExpect(status().isOk());
    }

    @Test
    void cumplimientoMetricas_SinFechas_UsaDefault() throws Exception {
        // Probamos que el endpoint maneje bien la falta de fechas
        when(reporteService.exportarCumplimientoCsv(any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(new byte[0]);

        mockMvc.perform(get("/api/reportes/cumplimiento/metricas/exportar"))
               .andExpect(status().isOk());
    }

}