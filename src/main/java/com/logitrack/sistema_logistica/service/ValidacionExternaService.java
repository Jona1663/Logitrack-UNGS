package com.logitrack.sistema_logistica.service;

import com.logitrack.sistema_logistica.model.Camion;
import com.logitrack.sistema_logistica.model.ChoferDetalle;
import com.logitrack.sistema_logistica.model.EmpresaCliente;
import com.logitrack.sistema_logistica.model.Establecimiento;
import com.logitrack.sistema_logistica.repository.CamionRepository;
import com.logitrack.sistema_logistica.repository.ChoferDetalleRepository;
import com.logitrack.sistema_logistica.repository.EmpresaClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Map;

@Service
@RequiredArgsConstructor // Usamos Lombok para inyectar dependencias limpiamente
public class ValidacionExternaService {

    private final RestTemplate restTemplate;
    private final CamionRepository camionRepository;
    private final EmpresaClienteRepository empresaClienteRepository;
    private final ChoferDetalleRepository choferDetalleRepository;

    @Value("${api.mock.base-url}")
    private String mockBaseUrl;

    public void verificarHabilitacionSenasa(LocalDate hoy, Camion camion) {
        if (camion.getVtoSenasa() != null && camion.getVtoSenasa().isBefore(hoy)) {
            try {
                String senasaUrl = mockBaseUrl + "/senasa/validar-camion/" + camion.getPatente();
                ResponseEntity<Map> responseSenasa = restTemplate.getForEntity(senasaUrl, Map.class);

                if (responseSenasa.getStatusCode().is2xxSuccessful() && responseSenasa.getBody() != null) {
                    String vtoSenasaStr = (String) responseSenasa.getBody().get("vencimientoHabilitacion");
                    camion.setVtoSenasa(LocalDate.parse(vtoSenasaStr));
                    camionRepository.save(camion);
                }
            } catch (HttpClientErrorException e) {
                throw new RuntimeException("Validación SENASA rechazada: El camión no está habilitado para transporte de granos.");
            } catch (Exception e) {
                throw new RuntimeException("Error de conexión al validar con SENASA.");
            }
        }
    }

    public void verificarRucaEmpresa(LocalDate hoy, Establecimiento origen) {
        EmpresaCliente empresa = origen.getEmpresa();
        if (empresa != null && empresa.getVtoRuca() != null && empresa.getVtoRuca().isBefore(hoy)) {
            try {
                String rucaUrl = mockBaseUrl + "/ruca/validar-empresa/" + empresa.getRucaNro();
                ResponseEntity<Map> responseRuca = restTemplate.getForEntity(rucaUrl, Map.class);

                if (responseRuca.getStatusCode().is2xxSuccessful() && responseRuca.getBody() != null) {
                    String vtoRucaStr = (String) responseRuca.getBody().get("vtoRucaNuevo");
                    empresa.setVtoRuca(LocalDate.parse(vtoRucaStr));
                    empresaClienteRepository.save(empresa);
                }
            } catch (HttpClientErrorException e) {
                throw new RuntimeException("Validación RUCA rechazada: La empresa dueña del origen está suspendida.");
            } catch (Exception e) {
                throw new RuntimeException("Error de conexión al validar RUCA.");
            }
        }
    }

    public void verificarLicenciaChofer(LocalDate hoy, ChoferDetalle chofer) {
        if (chofer.getVtoLicencia().isBefore(hoy) || chofer.getVtoLinti().isBefore(hoy)) {
            try {
                String cnrtUrl = mockBaseUrl + "/cnrt/validar-chofer/" + chofer.getNroLicencia();
                ResponseEntity<Map> responseCnrt = restTemplate.getForEntity(cnrtUrl, Map.class);

                if (responseCnrt.getStatusCode().is2xxSuccessful() && responseCnrt.getBody() != null) {
                    String vtoLicenciaStr = (String) responseCnrt.getBody().get("vtoLicenciaNuevo");
                    String vtoLintiStr = (String) responseCnrt.getBody().get("vtoLintiNuevo");

                    chofer.setVtoLicencia(LocalDate.parse(vtoLicenciaStr));
                    chofer.setVtoLinti(LocalDate.parse(vtoLintiStr));
                    choferDetalleRepository.save(chofer);
                }
            } catch (HttpClientErrorException e) {
                throw new RuntimeException("La CNRT informa que el chofer está inhabilitado para conducir.");
            } catch (Exception e) {
                throw new RuntimeException("Error al validar con CNRT.");
            }
        }
    }

    public String getNroAutorizacionArca(String cpe) { // Nota: le paso el String directo
        String nroAutorizacionArca = "";
        try {
            String arcaUrl = mockBaseUrl + "/arca/validar-cpe/" + cpe;
            ResponseEntity<Map> response = restTemplate.getForEntity(arcaUrl, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                nroAutorizacionArca = (String) response.getBody().get("nroAutorizacion");
            }
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Validación ARCA rechazada: El CPE es inválido o está inactivo.");
        } catch (Exception e) {
            throw new RuntimeException("Error de conexión con el servicio de ARCA.");
        }
        return nroAutorizacionArca;
    }
}