package com.logitrack.sistema_logistica.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.logitrack.sistema_logistica.dto.IncidenciaMapaDTO;
import com.logitrack.sistema_logistica.repository.IncidenciaRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IncidenciaMapaService {
private final IncidenciaRepository incidenciaRepository;

    @Transactional(readOnly = true)
    public List<IncidenciaMapaDTO> obtenerDatosMapaHistorico() {
        // Al llamar a este método, la consulta optimizada hace todo el trabajo pesado.
        // Si hay 10.000 incidencias, esto se ejecuta en milisegundos y no consume casi RAM.
        return incidenciaRepository.obtenerIncidenciasOptimizadasParaMapa();
    }
}

