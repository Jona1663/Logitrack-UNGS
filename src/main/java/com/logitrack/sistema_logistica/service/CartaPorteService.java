package com.logitrack.sistema_logistica.service;

import com.logitrack.sistema_logistica.repository.EnvioRepository;
import org.springframework.transaction.annotation.Transactional;
import com.logitrack.sistema_logistica.dto.CartaPorteDTO;
import com.logitrack.sistema_logistica.model.Envio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CartaPorteService {
    @Autowired
    private  EnvioRepository envioRepository;

    @Transactional(readOnly = true)
    public CartaPorteDTO obtenerCartaPorte(String idEnvio) {
        // Buscamos el envío principal
        Envio envio = envioRepository.findById(idEnvio)
                .orElseThrow(() -> new RuntimeException("No se encontró el envío con ID: " + idEnvio));

        // Validaciones preventivas para evitar NullPointerExceptions
        String patente = (envio.getCamion() != null) ? envio.getCamion().getPatente() : "S/D";
        
        String nombreChofer = "S/D";
        String cuil = "S/D";
        String licencia = "S/D";
        
        if (envio.getChofer() != null && envio.getChofer().getPersonaAsociada() != null) {
            nombreChofer = envio.getChofer().getPersonaAsociada().getNombre() + " " + envio.getChofer().getPersonaAsociada().getApellido();
            cuil = envio.getChofer().getPersonaAsociada().getCuil(); 
            licencia = envio.getChofer().getNroLicencia(); 
        }

        String nombreOrigen = (envio.getOrigen() != null) ? envio.getOrigen().getNombreLugar() : "S/D";
        String nombreDestino = (envio.getDestino() != null) ? envio.getDestino().getNombreLugar() : "S/D";

        // Construimos y retornamos el DTO
        return CartaPorteDTO.builder()
                .idEnvio(envio.getIdEnvio())
                .cpe(envio.getCpe() != null ? envio.getCpe() : "Pendiente de ARCA")
                .autorizacionArca(envio.getAutorizacionARCA() != null ? envio.getAutorizacionARCA() : "Pendiente")
                .patenteCamion(patente)
                .nombreChofer(nombreChofer)
                .cuilChofer(cuil)
                .licenciaChofer(licencia)
                .tipoGrano(envio.getTipoGrano() != null ? envio.getTipoGrano().name() : "S/D")
                .pesoEstimadoKg(envio.getKgOrigen()) 
                .origen(nombreOrigen)
                .destino(nombreDestino)
                .build();
    }
}
