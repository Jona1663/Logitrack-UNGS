package com.logitrack.sistema_logistica.service;

import com.logitrack.sistema_logistica.dto.ReporteCumplimientoResponse;
import com.logitrack.sistema_logistica.repository.IncidenciaRepository;
import com.logitrack.sistema_logistica.dto.CumplimientoMetricasDTO;
import com.logitrack.sistema_logistica.dto.MetricasCumplimientoDTO;
import com.logitrack.sistema_logistica.repository.EnvioRepository;
import com.logitrack.sistema_logistica.dto.ReporteEnvioDetalleDTO;
import org.springframework.transaction.annotation.Transactional;
import com.logitrack.sistema_logistica.dto.ReporteEficienciaDTO;
import com.logitrack.sistema_logistica.dto.ViajeCumplimientoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import com.logitrack.sistema_logistica.dto.ReporteEstadoDTO;
import com.logitrack.sistema_logistica.dto.ReporteSimpleDTO;
import com.logitrack.sistema_logistica.dto.ReporteGranoDTO;
import com.logitrack.sistema_logistica.model.Envio;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import java.time.temporal.ChronoUnit;
import org.apache.poi.ss.usermodel.*;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.io.IOException;
import java.util.List;

@Service
public class ReporteService {

        @Autowired
        private EnvioRepository envioRepository;

        @Autowired
        private IncidenciaRepository incidenciaRepository;

        @Transactional(readOnly = true)
        public ReporteSimpleDTO obtenerReporte(LocalDate fechaInicio, LocalDate fechaFin) {
                // --- CAMINO 1: Si el usuario envió fechas, usamos los filtros
                if (fechaInicio != null && fechaFin != null) {
                        LocalDateTime inicio = fechaInicio.atStartOfDay();
                        LocalDateTime fin = fechaFin.atTime(23, 59, 59);
                        
                        long totalViajes = envioRepository.countEntreFechas(inicio, fin);

                        // Validación para Empty State
                        if (totalViajes == 0) {
                                throw new RuntimeException("EMPTY_STATE");
                        }

                        Long totalKilos = envioRepository.sumKilosEntreFechas(inicio, fin);
                        long totalIncidencias = incidenciaRepository.countByFechaReporteBetween(inicio, fin);
                        
                        return ReporteSimpleDTO.builder()
                                .totalViajes(totalViajes).
                                totalKilos(totalKilos != null ? totalKilos : 0L)
                                .totalIncidencias(totalIncidencias)
                                .build();
                }


                //--- CAMINO 2: Histórico (Trae TODO el histórico)
                long totalViajes = envioRepository.count();

                // Validación para Empty State en histórico
                if (totalViajes == 0) {
                throw new RuntimeException("EMPTY_STATE");
                }

                Long totalKilos = envioRepository.sumKilos();
                long totalIncidencias = incidenciaRepository.count();

                return ReporteSimpleDTO.builder()
                        .totalViajes(totalViajes)
                        .totalKilos(totalKilos != null ? totalKilos : 0L )
                        .totalIncidencias(totalIncidencias)
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
    
                // 1. Obtenemos cuántos llegaron a tiempo y sus kilos
                long viajesATiempo = envioRepository.countEnviosATiempoEntreFechas(inicio, fin);
                long kilosATiempo = envioRepository.sumKilosATiempoEntreFechas(inicio, fin);

                // 2. Necesitamos el total de viajes completados para sacar el porcentaje real
                long totalCompletados = envioRepository.countCompletadosEntreFechas(inicio, fin);

                // 3. Calculamos el porcentaje de forma segura
                double porcentaje = 0.0;
                if (totalCompletados > 0) {
                        porcentaje = Math.round(((double) viajesATiempo / totalCompletados) * 100.0);
                }




                // Ahora llama al método que devuelve el DTO completo
                return ReporteEficienciaDTO.builder()
                .cantidadEnviosATiempo(viajesATiempo)
                .totalKilosEnTiempo(kilosATiempo)
                .porcentajeATiempo(porcentaje)
                .build();  
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
                throw new RuntimeException("EMPTY_STATE");
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
                // --- CRITERIO 1: Métricas Globales ---
                ReporteSimpleDTO totales = obtenerReporte(fechaInicio, fechaFin);

                // --- CUMPLIENDO EL PUNTO 4: Si no hay datos, lanzamos excepción para que el controlador devuelva un JSON ---
                if (totales == null || totales.getTotalViajes() == 0) {
                        throw new RuntimeException("EMPTY_STATE");
                }

                // --- CRITERIO 2: Estados ---
                List<ReporteEstadoDTO> estados = (fechaInicio != null && fechaFin != null) ? 
                        envioRepository.obtenerMetricasPorEstadoEntreFechas(fechaInicio.atStartOfDay(), fechaFin.atTime(23, 59, 59)) : 
                        envioRepository.obtenerMetricasPorEstado();

                java.util.Map<String, Long> mapaEstados = new java.util.HashMap<>();
                if (estados != null) {
                        for (ReporteEstadoDTO e : estados) {
                                if (e.getEstado() != null) {
                                        mapaEstados.put(e.getEstado().toString().toUpperCase(), e.getCantidadEnvios());
                                }
                        }
                }


                // --- CRITERIOS 3 y 4: Granos y Eficiencia ---
                // Para evitar errores si exportan todo el histórico sin fechas, asignamos un rango seguro.
                LocalDate inicioSeguro = (fechaInicio != null) ? fechaInicio : LocalDate.of(2000, 1, 1);
                LocalDate finSeguro = (fechaFin != null) ? fechaFin : LocalDate.now();

                List<ReporteGranoDTO> granos = obtenerReportePorGrano(inicioSeguro, finSeguro);
                ReporteEficienciaDTO eficiencia = obtenerMetricasATiempo(inicioSeguro, finSeguro);



                // BOM UTF-8 para Excel
                writer.write('\ufeff');


                // --- CUMPLIENDO EL PUNTO 3: Cambiamos al formato DEFAULT (separado por comas ,) ---
                org.apache.commons.csv.CSVFormat formato = org.apache.commons.csv.CSVFormat.DEFAULT.builder()
                        .setHeader("Métrica", "Valor")
                        .build();

                try (org.apache.commons.csv.CSVPrinter printer = new org.apache.commons.csv.CSVPrinter(writer, formato)) {
                        
                        // Imprimir Criterio 1
                        printer.printRecord("Viajes", totales.getTotalViajes());
                        printer.printRecord("Kilos Transportados", totales.getTotalKilos());
                        printer.printRecord("Incidencias", totales.getTotalIncidencias());
                        
                        // Imprimir Criterio 2
                        // --- CUMPLIENDO EL PUNTO 3: Filas fijas con los nombres exactos requeridos por el Frontend ---
                        // Usamos .getOrDefault para asegurar que si el estado tiene 0 viajes, imprima la fila con 0 en vez de desaparecer.
                        printer.printRecord("Estado: Pendientes", mapaEstados.getOrDefault("PENDIENTE", 0L));
                        printer.printRecord("Estado: En Tránsito", mapaEstados.getOrDefault("EN_TRANSITO", 0L));
                        printer.printRecord("Estado: En Punto de Recolección", mapaEstados.getOrDefault("EN_PUNTO_RECOLECCION", 0L));
                        printer.printRecord("Estado: Entregados", mapaEstados.getOrDefault("ENTREGADO", 0L));
                        printer.printRecord("Estado: Cancelados", mapaEstados.getOrDefault("CANCELADO", 0L));


                        // Imprimir Criterio 3
                        if (granos != null && !granos.isEmpty()) {
                                for (ReporteGranoDTO grano : granos) {
                                        printer.printRecord("Grano: " + grano.getTipoGrano(), grano.getCantidadEnvios());
                                }
                        } 

                        // Imprimir Criterio 4
                        if (eficiencia != null) {
                                printer.printRecord("Viajes a tiempo", eficiencia.getCantidadEnviosATiempo());
                                printer.printRecord("Kilos a tiempo", eficiencia.getTotalKilosEnTiempo());
                                printer.printRecord("Porcentaje a tiempo", eficiencia.getPorcentajeATiempo());
                        } else {
                                printer.printRecord("Viajes a tiempo", 0);
                                printer.printRecord("Kilos a tiempo", 0);
                                printer.printRecord("Porcentaje a tiempo", 0.0);
                        }

                        printer.flush();
                }
        }


        // =======================================================
        // NUEVOS MÉTODOS PARA EXCEL (#368)
        // =======================================================

        //vVERSION 1
        /* 
        @Transactional(readOnly = true)
        public java.io.ByteArrayInputStream generarExcelOperativo(LocalDate fechaInicio, LocalDate fechaFin) throws java.io.IOException {
                ReporteSimpleDTO totales = obtenerReporte(fechaInicio, fechaFin);
                // Nota: obtenerReporte() ya lanza "EMPTY_STATE" internamente si está vacío.

                List<ReporteEstadoDTO> estados = (fechaInicio != null && fechaFin != null) ? 
                        envioRepository.obtenerMetricasPorEstadoEntreFechas(fechaInicio.atStartOfDay(), fechaFin.atTime(23, 59, 59)) : 
                        envioRepository.obtenerMetricasPorEstado();

                java.util.Map<String, Long> mapaEstados = new java.util.HashMap<>();
                if (estados != null) {
                        for (ReporteEstadoDTO e : estados) {
                                if (e.getEstado() != null) mapaEstados.put(e.getEstado().toString().toUpperCase(), e.getCantidadEnvios());
                        }
                }

                try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(); 
                     java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
                        
                        org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Reporte Operativo");

                        org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
                        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
                        font.setBold(true);
                        headerStyle.setFont(font);

                        // Cabeceras
                        org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
                        headerRow.createCell(0).setCellValue("Métrica");
                        headerRow.createCell(1).setCellValue("Valor");
                        headerRow.getCell(0).setCellStyle(headerStyle);
                        headerRow.getCell(1).setCellStyle(headerStyle);

                        // Filas
                        sheet.createRow(1).createCell(0).setCellValue("Total de Viajes");
                        sheet.getRow(1).createCell(1).setCellValue(totales.getTotalViajes());

                        sheet.createRow(2).createCell(0).setCellValue("Kilos Transportados");
                        sheet.getRow(2).createCell(1).setCellValue(totales.getTotalKilos());

                        sheet.createRow(3).createCell(0).setCellValue("Total Incidencias");
                        sheet.getRow(3).createCell(1).setCellValue(totales.getTotalIncidencias());

                        int rowIdx = 4;
                        String[] estadosFijos = {"PENDIENTE", "EN_TRANSITO", "EN_PUNTO_RECOLECCION", "ENTREGADO", "CANCELADO"};
                        for (String estado : estadosFijos) {
                                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIdx++);
                                row.createCell(0).setCellValue("Estado: " + estado);
                                row.createCell(1).setCellValue(mapaEstados.getOrDefault(estado, 0L));
                        }

                        sheet.autoSizeColumn(0);
                        sheet.autoSizeColumn(1);

                        workbook.write(out);
                        return new java.io.ByteArrayInputStream(out.toByteArray());
                }
        }
        */


        //VERSION 2
        @Transactional(readOnly = true)
        public java.io.ByteArrayInputStream generarExcelOperativo(LocalDate fechaInicio, LocalDate fechaFin) throws java.io.IOException {              
                ReporteSimpleDTO totales = obtenerReporte(fechaInicio, fechaFin);
                if (totales == null || totales.getTotalViajes() == 0) {
                        throw new RuntimeException("EMPTY_STATE");
                }
                // Obtener datos
                List<ReporteEstadoDTO> estados = (fechaInicio != null && fechaFin != null) ? 
                        envioRepository.obtenerMetricasPorEstadoEntreFechas(fechaInicio.atStartOfDay(), fechaFin.atTime(23, 59, 59)) : 
                        envioRepository.obtenerMetricasPorEstado();
                java.util.Map<String, Long> mapaEstados = new java.util.HashMap<>();
                if (estados != null) {
                        for (ReporteEstadoDTO e : estados) {
                                if (e.getEstado() != null) {
                                        mapaEstados.put(e.getEstado().toString().toUpperCase(), e.getCantidadEnvios());
                                }
                        }
                }

                LocalDate inicioSeguro = (fechaInicio != null) ? fechaInicio : LocalDate.of(2000, 1, 1);
                LocalDate finSeguro = (fechaFin != null) ? fechaFin : LocalDate.now();
                List<ReporteGranoDTO> granos = obtenerReportePorGrano(inicioSeguro, finSeguro);
                ReporteEficienciaDTO eficiencia = obtenerMetricasATiempo(inicioSeguro, finSeguro);
                // Crear Excel en memoria
                try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(); 
                     java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {                 
                        org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Reporte Operativo");                    
                        // Cabecera en negrita
                        org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
                        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
                        font.setBold(true);
                        headerStyle.setFont(font);                    
                        org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
                        org.apache.poi.ss.usermodel.Cell cell0 = headerRow.createCell(0);
                        cell0.setCellValue("Métrica");
                        cell0.setCellStyle(headerStyle);                     
                        org.apache.poi.ss.usermodel.Cell cell1 = headerRow.createCell(1);
                        cell1.setCellValue("Valor");
                        cell1.setCellStyle(headerStyle);                       
                        // SOLUCIÓN: Usamos un array de 1 posición para poder incrementarlo dentro del Lambda
                        int[] rowNum = {1};
                        // Función auxiliar para agregar filas rápido
                        java.util.function.BiConsumer<String, Object> addRow = (metrica, valor) -> {
                                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum[0]++);
                                row.createCell(0).setCellValue(metrica);
                                if (valor instanceof Long) row.createCell(1).setCellValue((Long) valor);
                                else if (valor instanceof Integer) row.createCell(1).setCellValue((Integer) valor);
                                else if (valor instanceof Double) row.createCell(1).setCellValue((Double) valor);
                                else row.createCell(1).setCellValue(valor.toString());
                        };
                        // 1. Métricas Globales
                        addRow.accept("Viajes", totales.getTotalViajes());
                        addRow.accept("Kilos Transportados", totales.getTotalKilos());
                        addRow.accept("Incidencias", totales.getTotalIncidencias());
                        // 2. Estados
                        addRow.accept("Estado: Pendientes", mapaEstados.getOrDefault("PENDIENTE", 0L));
                        addRow.accept("Estado: En Tránsito", mapaEstados.getOrDefault("EN_TRANSITO", 0L));
                        addRow.accept("Estado: En Punto de Recolección", mapaEstados.getOrDefault("EN_PUNTO_RECOLECCION", 0L));
                        addRow.accept("Estado: Entregados", mapaEstados.getOrDefault("ENTREGADO", 0L));
                        addRow.accept("Estado: Cancelados", mapaEstados.getOrDefault("CANCELADO", 0L));
                        // 3. Granos
                        if (granos != null && !granos.isEmpty()) {
                                for (ReporteGranoDTO grano : granos) {
                                        addRow.accept("Grano: " + grano.getTipoGrano(), grano.getCantidadEnvios());
                                }
                        }
                        // 4. Eficiencia
                        if (eficiencia != null) {
                                addRow.accept("Viajes a tiempo", eficiencia.getCantidadEnviosATiempo());
                                addRow.accept("Kilos a tiempo", eficiencia.getTotalKilosEnTiempo());
                                addRow.accept("Porcentaje a tiempo", eficiencia.getPorcentajeATiempo());
                        } else {
                                addRow.accept("Viajes a tiempo", 0L);
                                addRow.accept("Kilos a tiempo", 0L);
                                addRow.accept("Porcentaje a tiempo", 0.0);
                        }
                        // Autoajustar columnas
                        sheet.autoSizeColumn(0);
                        sheet.autoSizeColumn(1);

                        workbook.write(out);
                        return new java.io.ByteArrayInputStream(out.toByteArray());
                }
        }



        @Transactional(readOnly = true)
        public java.io.ByteArrayInputStream generarExcelCumplimiento(LocalDate fechaInicio, LocalDate fechaFin) throws java.io.IOException {
                List<ViajeCumplimientoDTO> viajes = calcularDesviosYCumplimiento(fechaInicio, fechaFin);              
                if (viajes == null || viajes.isEmpty()) {
                        throw new RuntimeException("EMPTY_STATE");
                }
                try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(); 
                     java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {                    
                        org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Cumplimiento");
                        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                        org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
                        String[] columnas = {"ID Envío", "Estado", "ETA", "Entrega Real", "Retrasado", "Desvío"};                      
                        org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
                        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
                        font.setBold(true);
                        headerStyle.setFont(font);
                        for (int i = 0; i < columnas.length; i++) {
                                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                                cell.setCellValue(columnas[i]);
                                cell.setCellStyle(headerStyle);
                        }
                        int rowIdx = 1;
                        for (ViajeCumplimientoDTO viaje : viajes) {
                                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIdx++);
                                row.createCell(0).setCellValue(viaje.getIdEnvio());
                                row.createCell(1).setCellValue(viaje.getEstadoActual());
                                row.createCell(2).setCellValue(viaje.getFechaEstimadaLlegada() != null ? viaje.getFechaEstimadaLlegada().format(fmt) : "");
                                row.createCell(3).setCellValue(viaje.getFechaEntregaReal() != null ? viaje.getFechaEntregaReal().format(fmt) : "");
                                row.createCell(4).setCellValue(viaje.isEsRetrasado() ? "SÍ" : "NO");
                                row.createCell(5).setCellValue(formatearDesvio(viaje.getDesvioHoras()));
                        }
                        for (int i = 0; i < columnas.length; i++) {
                                sheet.autoSizeColumn(i);
                        }
                        workbook.write(out);
                        return new java.io.ByteArrayInputStream(out.toByteArray());
                }
        }


        // ===============================================================
        //                   MÉTODOS PARA DETALLE DE ENVÍOS 
        // ================================================================
        @Transactional(readOnly = true)
        public List<ReporteEnvioDetalleDTO> obtenerDetalleEnvios(LocalDate fechaInicio, LocalDate fechaFin) {
                LocalDateTime inicio = (fechaInicio != null) ? fechaInicio.atStartOfDay() : LocalDate.of(2000, 1, 1).atStartOfDay();
                LocalDateTime fin = (fechaFin != null) ? fechaFin.atTime(23, 59, 59) : LocalDateTime.now();
                // 1. Usamos el método de Stream que funciona bien
                List<Envio> envios = new java.util.ArrayList<>();
                try (java.util.stream.Stream<Envio> streamEnvios = envioRepository.obtenerEnviosComoStreamParaExportacion(inicio, fin)) {
                        envios = streamEnvios.collect(java.util.stream.Collectors.toList());
                }          
                return envios.stream().map(envio -> {
                        // 1. Extraemos y parseamos los Kilos
                        Long kilos = envio.getKgDestino() != null ? envio.getKgDestino().longValue() : 
                                    (envio.getKgOrigen() != null ? envio.getKgOrigen().longValue() : 0L);
                        // 2. Extraemos y parseamos el Estado a String de forma segura
                        String estadoStr = envio.getEstadoActual() != null ? envio.getEstadoActual().name() : "N/A";
                        // 3. Extraemos y parseamos el Grano a String de forma segura
                        String granoStr = envio.getTipoGrano() != null ? envio.getTipoGrano().toString() : "N/A";
                        // 4. Armamos el DTO (Ahora Java entiende perfectamente los tipos de datos)
                        return ReporteEnvioDetalleDTO.builder()
                                        .idEnvio(envio.getIdEnvio())
                                        .estado(estadoStr)
                                        .kilosTransportados(kilos)
                                        .tipoGrano(granoStr)
                                        .fechaEstimadaLlegada(envio.getFechaEstimadaLlegada())
                                        .fechaLlegada(envio.getFechaLlegada())
                                        .build();
                }).collect(java.util.stream.Collectors.toList());
        }       

        @Transactional(readOnly = true)
        public void exportarDetalleEnviosCsv(LocalDate fechaInicio, LocalDate fechaFin, java.io.Writer writer) throws java.io.IOException {
                List<ReporteEnvioDetalleDTO> detalles = obtenerDetalleEnvios(fechaInicio, fechaFin);
                writer.write('\ufeff'); // BOM para que Excel reconozca los caracteres especiales
                org.apache.commons.csv.CSVFormat formato = org.apache.commons.csv.CSVFormat.DEFAULT.builder()
                                .setHeader("ID Envío", "Estado", "Kilos Transportados", "Tipo de Grano", "Fecha Estimada Llegada", "Fecha Llegada Real")
                                .build();
                java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                try (org.apache.commons.csv.CSVPrinter printer = new org.apache.commons.csv.CSVPrinter(writer, formato)) {
                        for (ReporteEnvioDetalleDTO dto : detalles) {
                                printer.printRecord(
                                                dto.getIdEnvio(),
                                                dto.getEstado(),
                                                dto.getKilosTransportados(),
                                                dto.getTipoGrano(),
                                                dto.getFechaEstimadaLlegada() != null ? dto.getFechaEstimadaLlegada().format(dtf) : "-",
                                                dto.getFechaLlegada() != null ? dto.getFechaLlegada().format(dtf) : "-"
                                );
                        }
                        printer.flush();
                }
        }   
        
        @Transactional(readOnly = true)
        public java.io.ByteArrayInputStream generarExcelDetalleEnvios(LocalDate fechaInicio, LocalDate fechaFin) throws java.io.IOException {
                List<ReporteEnvioDetalleDTO> detalles = obtenerDetalleEnvios(fechaInicio, fechaFin);
                try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(); 
                     java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {               
                        org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Detalle de Envíos");                     
                        org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
                        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
                        font.setBold(true);
                        headerStyle.setFont(font);                     
                        String[] columnas = {"ID Envío", "Estado", "Kilos Transportados", "Tipo de Grano", "Fecha Estimada Llegada", "Fecha Llegada Real"};
                        org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
                        for (int i = 0; i < columnas.length; i++) {
                                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                                cell.setCellValue(columnas[i]);
                                cell.setCellStyle(headerStyle);
                        }                     
                        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                        int rowNum = 1;
                        for (ReporteEnvioDetalleDTO dto : detalles) {
                                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                                row.createCell(0).setCellValue(dto.getIdEnvio());
                                row.createCell(1).setCellValue(dto.getEstado());
                                row.createCell(2).setCellValue(dto.getKilosTransportados() != null ? dto.getKilosTransportados() : 0);
                                row.createCell(3).setCellValue(dto.getTipoGrano());
                                row.createCell(4).setCellValue(dto.getFechaEstimadaLlegada() != null ? dto.getFechaEstimadaLlegada().format(dtf) : "-");
                                row.createCell(5).setCellValue(dto.getFechaLlegada() != null ? dto.getFechaLlegada().format(dtf) : "-");
                        }
                        for (int i = 0; i < columnas.length; i++) {
                                sheet.autoSizeColumn(i);
                        }
                        workbook.write(out);
                        return new java.io.ByteArrayInputStream(out.toByteArray());
                }
        }        

        // ===============================================================
        //                   MÉTODOS PARA MÉTRICAS DE CUMPLIMIENTO              
        // ===============================================================
        //version1
        /* 
        @Transactional(readOnly = true)
        public CumplimientoMetricasDTO obtenerMetricasCumplimiento() {
                long total = envioRepository.countTotalEntregados();
                long aTiempo = envioRepository.countEntregadosATiempo();
                long conRetraso = envioRepository.countConRetraso();

                // Cálculos de porcentaje con protección contra división por cero
                double pctATiempo = (total == 0) ? 0.0 : Math.round(((double) aTiempo / total * 100) * 100.0) / 100.0;
                double pctRetraso = (total == 0) ? 0.0 : Math.round(((double) conRetraso / total * 100) * 100.0) / 100.0;

                return CumplimientoMetricasDTO.builder()
                        .totalEntregados(total)
                        .entregadosATiempo(aTiempo)
                        .conRetraso(conRetraso)
                        .porcentajeATiempo(pctATiempo)
                        .porcentajeConRetraso(pctRetraso)
                        .build();
        }

        public byte[] exportarCumplimientoCsv() {
                CumplimientoMetricasDTO metricas = obtenerMetricasCumplimiento();
                
                // Al ser una sola fila, armar el CSV manualmente es muy rápido
                StringBuilder sb = new StringBuilder();
                sb.append("Total Entregados,Entregados a Tiempo,Con Retraso,% a Tiempo,% con Retraso\n");
                sb.append(metricas.getTotalEntregados()).append(",")
                .append(metricas.getEntregadosATiempo()).append(",")
                .append(metricas.getConRetraso()).append(",")
                .append(metricas.getPorcentajeATiempo()).append("% ,")
                .append(metricas.getPorcentajeConRetraso()).append("%\n");

                return sb.toString().getBytes(StandardCharsets.UTF_8);
        }     
        
        public byte[] exportarCumplimientoExcel() {
        CumplimientoMetricasDTO metricas = obtenerMetricasCumplimiento();

        try (Workbook workbook = new XSSFWorkbook(); 
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                
                Sheet sheet = workbook.createSheet("Métricas de Cumplimiento");

                // 1. Crear la fila de encabezados (Fila 0)
                Row headerRow = sheet.createRow(0);
                String[] columnas = {"Total Entregados", "Entregados a Tiempo", "Con Retraso", "% a Tiempo", "% con Retraso"};
                
                // Estilo pro para los encabezados (Negrita)
                CellStyle headerStyle = workbook.createCellStyle();
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);

                for (int i = 0; i < columnas.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columnas[i]);
                cell.setCellStyle(headerStyle);
                }

                // 2. Crear la fila de datos (Fila 1)
                Row dataRow = sheet.createRow(1);
                dataRow.createCell(0).setCellValue(metricas.getTotalEntregados());
                dataRow.createCell(1).setCellValue(metricas.getEntregadosATiempo());
                dataRow.createCell(2).setCellValue(metricas.getConRetraso());
                
                // Concatenamos el "%" para que se muestre directo como texto formateado
                dataRow.createCell(3).setCellValue(metricas.getPorcentajeATiempo() + "%");
                dataRow.createCell(4).setCellValue(metricas.getPorcentajeConRetraso() + "%");

                // 3. Autoajustar el ancho de las columnas para que no se corte el texto
                for (int i = 0; i < columnas.length; i++) {
                sheet.autoSizeColumn(i);
                }

                // Escribir los datos en el flujo de bytes
                workbook.write(out);
                return out.toByteArray();

                } catch (IOException e) {
                        throw new RuntimeException("Error crítico al generar el archivo Excel de cumplimiento", e);
                }
        }
        */

        //Version2
        @Transactional(readOnly = true)
                public CumplimientoMetricasDTO obtenerMetricasCumplimiento(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
                long total = envioRepository.countTotalEntregados(fechaInicio, fechaFin);
                long aTiempo = envioRepository.countEntregadosATiempo(fechaInicio, fechaFin);
                long conRetraso = envioRepository.countConRetraso(fechaInicio, fechaFin);

                double pctATiempo = (total == 0) ? 0.0 : Math.round(((double) aTiempo / total * 100) * 100.0) / 100.0;
                double pctRetraso = (total == 0) ? 0.0 : Math.round(((double) conRetraso / total * 100) * 100.0) / 100.0;

                return CumplimientoMetricasDTO.builder()
                        .totalEntregados(total)
                        .entregadosATiempo(aTiempo)
                        .conRetraso(conRetraso)
                        .porcentajeATiempo(pctATiempo)
                        .porcentajeConRetraso(pctRetraso)
                        .build();
        }

        // Actualiza los métodos de exportación para que pasen los parámetros
        public byte[] exportarCumplimientoCsv(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
                CumplimientoMetricasDTO metricas = obtenerMetricasCumplimiento(fechaInicio, fechaFin);
                // ... el resto de la generación del CSV queda igual ...
                StringBuilder sb = new StringBuilder();
                sb.append("Total Entregados,Entregados a Tiempo,Con Retraso,% a Tiempo,% con Retraso\n");
                sb.append(metricas.getTotalEntregados()).append(",")
                .append(metricas.getEntregadosATiempo()).append(",")
                .append(metricas.getConRetraso()).append(",")
                .append(metricas.getPorcentajeATiempo()).append("%,")
                .append(metricas.getPorcentajeConRetraso()).append("%\n");
                return sb.toString().getBytes(StandardCharsets.UTF_8);
        }    

        public byte[] exportarCumplimientoExcel(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        CumplimientoMetricasDTO metricas = obtenerMetricasCumplimiento( fechaInicio, fechaFin);

        try (Workbook workbook = new XSSFWorkbook(); 
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                
                Sheet sheet = workbook.createSheet("Métricas de Cumplimiento");

                // 1. Crear la fila de encabezados (Fila 0)
                Row headerRow = sheet.createRow(0);
                String[] columnas = {"Total Entregados", "Entregados a Tiempo", "Con Retraso", "% a Tiempo", "% con Retraso"};
                
                // Estilo pro para los encabezados (Negrita)
                CellStyle headerStyle = workbook.createCellStyle();
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);

                for (int i = 0; i < columnas.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columnas[i]);
                cell.setCellStyle(headerStyle);
                }

                // 2. Crear la fila de datos (Fila 1)
                Row dataRow = sheet.createRow(1);
                dataRow.createCell(0).setCellValue(metricas.getTotalEntregados());
                dataRow.createCell(1).setCellValue(metricas.getEntregadosATiempo());
                dataRow.createCell(2).setCellValue(metricas.getConRetraso());
                
                // Concatenamos el "%" para que se muestre directo como texto formateado
                dataRow.createCell(3).setCellValue(metricas.getPorcentajeATiempo() + "%");
                dataRow.createCell(4).setCellValue(metricas.getPorcentajeConRetraso() + "%");

                // 3. Autoajustar el ancho de las columnas para que no se corte el texto
                for (int i = 0; i < columnas.length; i++) {
                sheet.autoSizeColumn(i);
                }

                // Escribir los datos en el flujo de bytes
                workbook.write(out);
                return out.toByteArray();

                } catch (IOException e) {
                        throw new RuntimeException("Error crítico al generar el archivo Excel de cumplimiento", e);
                }
        }



}
