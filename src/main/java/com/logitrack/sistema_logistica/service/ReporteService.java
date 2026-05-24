package com.logitrack.sistema_logistica.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

        // 2. Iteramos cada envío para calcular la diferencia exacta contra el ETA
        for (Envio envio : enviosCompletados) {
                // Calculamos la diferencia en minutos entre la entrega real y el ETA
                // Si el resultado es positivo, significa que llegó después del ETA (Retraso)
                long minutosDiferencia = ChronoUnit.MINUTES.between(envio.getFechaEstimadaLlegada(), envio.getFechaLlegada());
                
                double horasDesvio = 0.0;
                boolean esRetrasado = false;

                if (minutosDiferencia > 0) {
                // Pasamos los minutos a horas con decimales (ej: 90 minutos -> 1.5 horas)
                horasDesvio = minutosDiferencia / 60.0;
                esRetrasado = true;
                }

                // 3. Mapeamos los datos calculados al DTO individual
                ViajeCumplimientoDTO dto = ViajeCumplimientoDTO.builder()
                        .idEnvio(envio.getIdEnvio()) // O el campo identificador que uses
                        .estadoActual(envio.getEstadoActual().toString())
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



    

}
