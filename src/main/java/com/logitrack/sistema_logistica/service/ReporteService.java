package com.logitrack.sistema_logistica.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logitrack.sistema_logistica.dto.ReporteEficienciaDTO;
import com.logitrack.sistema_logistica.dto.ReporteEstadoDTO;
import com.logitrack.sistema_logistica.dto.ReporteGranoDTO;
import com.logitrack.sistema_logistica.dto.ReporteSimpleDTO;
import com.logitrack.sistema_logistica.repository.EnvioRepository;

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




    

}
