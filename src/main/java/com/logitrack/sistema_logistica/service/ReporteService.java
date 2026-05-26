package com.logitrack.sistema_logistica.service;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logitrack.sistema_logistica.dto.MetricasCumplimientoDTO;
import com.logitrack.sistema_logistica.dto.ReporteCumplimientoResponse;
import com.logitrack.sistema_logistica.dto.ReporteEficienciaDTO;
import com.logitrack.sistema_logistica.dto.ReporteEstadoDTO;
import com.logitrack.sistema_logistica.dto.ReporteGranoDTO;
import com.logitrack.sistema_logistica.dto.ReporteSimpleDTO;
import com.logitrack.sistema_logistica.dto.ViajeCumplimientoDTO;
import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.repository.EnvioRepository;

import java.time.temporal.ChronoUnit;
import com.logitrack.sistema_logistica.dto.ViajeCumplimientoDTO;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import java.io.StringWriter;
import java.io.IOException;

@Service
public class ReporteService {

        @Autowired
        private EnvioRepository envioRepository;

        @Transactional(readOnly = true)
        public ReporteSimpleDTO obtenerReporte(LocalDate fechaInicio, LocalDate fechaFin) {
                // Si el usuario envió fechas, usamos los filtros
                if (fechaInicio != null && fechaFin != null) {
                LocalDateTime inicio = fechaInicio.atStartOfDay();
                LocalDateTime fin = fechaFin.atTime(23, 59, 59);
                
                long totalViajes = envioRepository.countEntreFechas(inicio, fin);
                Long totalKilos = envioRepository.sumKilosEntreFechas(inicio, fin);
                return ReporteSimpleDTO.builder().totalViajes(totalViajes).totalKilos(totalKilos != null ? totalKilos : 0L).build();
                }


                //(Trae TODO el histórico)
                long totalViajes = envioRepository.count();
                Long totalKilos = envioRepository.sumKilos();
                if (totalKilos == null) {
                totalKilos = 0L;
                }

                return ReporteSimpleDTO.builder()
                .totalViajes(totalViajes)
                .totalKilos(totalKilos)
                .build();
        }


        @Transactional(readOnly = true)
        public List<ReporteEstadoDTO> obtenerReportePorEstados(String rango) {

                // Si nos piden los últimos 7 días
                if (rango != null && rango.equalsIgnoreCase("ULTIMOS_7_DIAS")) {
                LocalDateTime fin = LocalDateTime.now();
                LocalDateTime inicio = fin.minusDays(7);
                return envioRepository.obtenerMetricasPorEstadoEntreFechas(inicio, fin);
                }

                //(Trae TODO el histórico)
                return envioRepository.obtenerMetricasPorEstado();
        }
        
        @Transactional(readOnly = true)
        public List<ReporteEstadoDTO> obtenerReportePorEstadosPorFechas(LocalDate fechaInicio, LocalDate fechaFin) {
                LocalDateTime inicio = fechaInicio.atStartOfDay();
                LocalDateTime fin = fechaFin.atTime(23, 59, 59);
                return envioRepository.obtenerMetricasPorEstadoEntreFechas(inicio, fin);
        }

        // Para obtener métricas por grano
        @Transactional(readOnly = true)
        public List<ReporteGranoDTO> obtenerReportePorGrano(LocalDate fechaInicio, LocalDate fechaFin) {
                LocalDateTime inicio = fechaInicio.atStartOfDay();
                LocalDateTime fin = fechaFin.atTime(23, 59, 59);
                return envioRepository.obtenerMetricasPorGrano(inicio, fin);
        }

        // Para obtener métricas de eficiencia (a tiempo)
        @Transactional(readOnly = true)
        public ReporteEficienciaDTO obtenerMetricasATiempo(LocalDate fechaInicio, LocalDate fechaFin) {
                LocalDateTime inicio = fechaInicio.atStartOfDay();
                LocalDateTime fin = fechaFin.atTime(23, 59, 59);
    
        // Ahora llama al método que devuelve el DTO completo
        return envioRepository.obtenerMetricasATiempo(inicio, fin);    
        }



        //#237
        @Transactional(readOnly = true)
        public List<ViajeCumplimientoDTO> calcularDesviosYCumplimiento(LocalDate fechaInicio, LocalDate fechaFin) {
        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atTime(23, 59, 59);

        // 1. Buscamos solo los envíos completados del repositorio
        List<Envio> enviosCompletados = envioRepository.obtenerEnviosCompletadosParaCumplimiento(inicio, fin);
        List<ViajeCumplimientoDTO> viajesProcesados = new ArrayList<>();


        /*
        // ⚠️ LÍNEAS TEMPORALES PARA PROBAR EL BUG #238 
        if (enviosCompletados.isEmpty()) {
        Envio envioFalsoConEstadoNulo = Envio.builder()
            .idEnvio("LT-TEST-NULL")
            .estadoActual(null) // <-- Forzamos el estado en nulo
            .fechaEstimadaLlegada(LocalDateTime.now())
            .fechaLlegada(LocalDateTime.now().plusHours(2))
            .build();
        enviosCompletados = new java.util.ArrayList<>();
        enviosCompletados.add(envioFalsoConEstadoNulo);
}       */

        // 2. Iteramos cada envío para calcular la diferencia exacta contra el ETA
        for (Envio envio : enviosCompletados) {

                // Salto seguro por si viene un objeto nulo de la lista
                if (envio == null) continue;    

                // Calculamos la diferencia en minutos entre la entrega real y el ETA
                long minutosDiferencia = 0;
                if (envio.getFechaEstimadaLlegada() != null && envio.getFechaLlegada() != null) {
                        minutosDiferencia = ChronoUnit.MINUTES.between(envio.getFechaEstimadaLlegada(), envio.getFechaLlegada());
                }

                double horasDesvio = 0.0;
                boolean esRetrasado = false;

                if (minutosDiferencia > 0) {
                        // Pasamos los minutos a horas con decimales (ej: 90 minutos -> 1.5 horas)
                        horasDesvio = minutosDiferencia / 60.0;
                        esRetrasado = true;
                }

                // --- SOLUCIÓN AL BUG #238 ---
                // Evaluamos de forma segura si el estado es nulo antes de transformarlo a String
                String estadoFormateado = (envio.getEstadoActual() != null) 
                        ? envio.getEstadoActual().toString() 
                        : "DESCONOCIDO";


                // 3. Mapeamos los datos calculados al DTO individual
                ViajeCumplimientoDTO dto = ViajeCumplimientoDTO.builder()
                        .idEnvio(envio.getIdEnvio()) // O el campo identificador que uses
                        .estadoActual(estadoFormateado)
                        .fechaEstimadaLlegada(envio.getFechaEstimadaLlegada())
                        .fechaEntregaReal(envio.getFechaLlegada())
                        .esRetrasado(esRetrasado)
                        .desvioHoras(horasDesvio)
                        .build();

                viajesProcesados.add(dto);
        }

        return viajesProcesados;
        }        




        @Transactional(readOnly = true)
        public ReporteCumplimientoResponse obtenerReporteCumplimiento(LocalDate fechaInicio, LocalDate fechaFin) {
        // 1. Obtenemos los viajes individuales con sus desvíos ya calculados (lo que hicimos en el Paso 3)
        List<ViajeCumplimientoDTO> viajes = calcularDesviosYCumplimiento(fechaInicio, fechaFin);

        long totalEntregados = viajes.size();
        long entregadosATiempo = 0;
        long entregadosConRetraso = 0;

        // 2. Contamos cuántos llegaron a tiempo y cuántos con retraso
        for (ViajeCumplimientoDTO viaje : viajes) {
                if (viaje.isEsRetrasado()) {
                entregadosConRetraso++;
                } else {
                entregadosATiempo++;
                }
        }

        // 3. Calculamos los porcentajes de forma segura (evitando dividir por cero)
        double porcentajeATiempo = 0.0;
        double porcentajeRetraso = 0.0;

        if (totalEntregados > 0) {
                porcentajeATiempo = Math.round(((double) entregadosATiempo / totalEntregados) * 100.0);
                porcentajeRetraso = Math.round(((double) entregadosConRetraso / totalEntregados) * 100.0);
        }

        // 4. Armamos el DTO de métricas globales
        MetricasCumplimientoDTO metricas = MetricasCumplimientoDTO.builder()
                .totalEntregados(totalEntregados)
                .entregadosATiempo(entregadosATiempo)
                .entregadosConRetraso(entregadosConRetraso)
                .porcentajeATiempo(porcentajeATiempo)
                .porcentajeRetraso(porcentajeRetraso)
                .build();

        // 5. Retornamos la respuesta consolidada final
        return ReporteCumplimientoResponse.builder()
                .metricas(metricas)
                .viajes(viajes)
                .build();
        }



        // Método adicional para exportar a CSV (Paso 4)
        // Este método reutiliza el cálculo de desvíos y cumplimiento para generar un CSV descargable
        // El CSV tendrá columnas: ID Envío, Estado, Fecha ETA, Fecha Entrega Real, Retrasado (SI/NO), Desvío en Horas
        // El formato de fecha en el CSV será "dd/MM/yyyy HH:mm" para que sea legible en Excel
        // El método devuelve un String con el contenido del CSV, que luego el controlador puede enviar como respuesta con el tipo de contenido adecuado
        // Ejemplo de uso en el controlador: GET /api/reportes/cumplimiento/exportar?fechaInicio=2024-01-01&fechaFin=2024-01-31
        // El controlador llamaría a este método para obtener el CSV y luego lo enviaría con el header "Content-Disposition: attachment; filename=reporte_cumplimiento.csv"
        // Nota: Este método no maneja la respuesta HTTP directamente, solo genera el contenido del CSV. El controlador es responsable de configurar los headers y el tipo de contenido.

        // -formateo de horas como lo pide el frontend
        private String formatearDesvio(double horas) {
                if (horas <= 0) return "A tiempo";
                int dias = (int) (horas / 24);
                int horasRestantes = (int) (horas % 24);
                if (dias > 0 && horasRestantes > 0) return dias + " d " + horasRestantes + " h de retraso";
                if (dias > 0) return dias + " d de retraso";
                return horasRestantes + " h de retraso";
        }

        // export en fformato que solicita el frontend
        @Transactional(readOnly = true)
        public void exportarViajesCumplimientoStreamCsv(LocalDate fechaInicio, LocalDate fechaFin, java.io.Writer writer) throws java.io.IOException {
                LocalDateTime inicio = fechaInicio.atStartOfDay();
                LocalDateTime fin = fechaFin.atTime(23, 59, 59);
                java.time.format.DateTimeFormatter formateador = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

                // Validación de datos para devolver error 400 si está vacío
                long count = envioRepository.countEntreFechas(inicio, fin);
                if (count == 0) {
                throw new RuntimeException("No hay viajes completados para exportar en este período.");
                }

                // BOM UTF-8 para Excel
                writer.write('\ufeff');

                org.apache.commons.csv.CSVFormat formato = org.apache.commons.csv.CSVFormat.EXCEL.builder()
                        .setDelimiter(';')
                        .setHeader("ID Envío", "Estado", "ETA (Estimado)", "Entrega Real", "Desvío")
                        .build();

                try (java.util.stream.Stream<Envio> streamEnvios = envioRepository.obtenerEnviosComoStreamParaExportacion(inicio, fin);
                org.apache.commons.csv.CSVPrinter printer = new org.apache.commons.csv.CSVPrinter(writer, formato)) {

                streamEnvios
                        .filter(envio -> envio.getEstadoActual() != null && envio.getEstadoActual().name().equals("ENTREGADO"))
                        .forEach(envio -> {
                        try {
                                double desvioHoras = 0;
                                if (envio.getFechaLlegada() != null && envio.getFechaEstimadaLlegada() != null) {
                                long minutosDiferencia = java.time.temporal.ChronoUnit.MINUTES.between(
                                        envio.getFechaEstimadaLlegada(), envio.getFechaLlegada());
                                if (minutosDiferencia > 0) {
                                        desvioHoras = minutosDiferencia / 60.0;
                                }
                                }

                                printer.printRecord(
                                        envio.getIdEnvio(),
                                        envio.getEstadoActual().name(),
                                        envio.getFechaEstimadaLlegada() != null ? envio.getFechaEstimadaLlegada().format(formateador) : "",
                                        envio.getFechaLlegada() != null ? envio.getFechaLlegada().format(formateador) : "",
                                        formatearDesvio(desvioHoras)
                                );
                                printer.flush();
                        } catch (java.io.IOException e) {
                                throw new RuntimeException("Error escribiendo fila en CSV", e);
                        }
                        });
                }
        }

        // formato de reporte que solicita el frontend
        @Transactional(readOnly = true)
        public void exportarReporteOperativoCsv(LocalDate fechaInicio, LocalDate fechaFin, java.io.Writer writer) throws java.io.IOException {
                ReporteSimpleDTO totales = obtenerReporte(fechaInicio, fechaFin);
                List<ReporteEstadoDTO> estados = (fechaInicio != null && fechaFin != null) ? 
                        envioRepository.obtenerMetricasPorEstadoEntreFechas(fechaInicio.atStartOfDay(), fechaFin.atTime(23, 59, 59)) : 
                        envioRepository.obtenerMetricasPorEstado();

                // BOM UTF-8 para Excel
                writer.write('\ufeff');

                org.apache.commons.csv.CSVFormat formato = org.apache.commons.csv.CSVFormat.EXCEL.builder()
                        .setDelimiter(';')
                        .setHeader("Métrica", "Valor")
                        .build();

                try (org.apache.commons.csv.CSVPrinter printer = new org.apache.commons.csv.CSVPrinter(writer, formato)) {
                printer.printRecord("Total de Viajes", totales.getTotalViajes());
                printer.printRecord("Kilos Transportados (kg)", totales.getTotalKilos());
                
                for (ReporteEstadoDTO e : estados) {
                        printer.printRecord("Estado: " + e.getEstado(), e.getCantidadEnvios());
                }
                printer.flush();
                }
        }






}
