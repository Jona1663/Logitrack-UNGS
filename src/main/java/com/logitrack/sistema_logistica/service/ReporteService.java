package com.logitrack.sistema_logistica.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logitrack.sistema_logistica.dto.ReporteSimpleDTO;
import com.logitrack.sistema_logistica.repository.EnvioRepository;

@Service
public class ReporteService {

    @Autowired
    private EnvioRepository envioRepository;

    @Transactional(readOnly = true)
    public ReporteSimpleDTO obtenerReporte() {
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
}
