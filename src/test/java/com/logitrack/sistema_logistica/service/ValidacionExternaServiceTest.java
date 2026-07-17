package com.logitrack.sistema_logistica.service;

import com.logitrack.sistema_logistica.model.Camion;
import com.logitrack.sistema_logistica.model.ChoferDetalle;
import com.logitrack.sistema_logistica.model.EmpresaCliente;
import com.logitrack.sistema_logistica.model.Establecimiento;
import com.logitrack.sistema_logistica.repository.CamionRepository;
import com.logitrack.sistema_logistica.repository.ChoferDetalleRepository;
import com.logitrack.sistema_logistica.repository.EmpresaClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ValidacionExternaServiceTest {

    @Mock private RestTemplate restTemplate;
    @Mock private CamionRepository camionRepository;
    @Mock private EmpresaClienteRepository empresaClienteRepository;
    @Mock private ChoferDetalleRepository choferDetalleRepository;

    @InjectMocks
    private ValidacionExternaService validacionService;

    @BeforeEach
    void setUp() {
        // Inyectamos la URL base simulada para que los tests no fallen por NullPointer en la concatenación
        ReflectionTestUtils.setField(validacionService, "mockBaseUrl", "http://mock-api.com");
    }

    // ==========================================
    // TESTS PARA SENASA (Camiones)
    // ==========================================

    @Test
    public void verificarHabilitacionSenasa_CuandoVenceHoy_DeberiaLlamarApiYActualizar() {
        LocalDate hoy = LocalDate.now();
        Camion camion = new Camion();
        camion.setPatente("ABC-123");
        camion.setVtoSenasa(hoy.minusDays(1)); // Vencida ayer

        Map<String, String> responseBody = Map.of("vencimientoHabilitacion", hoy.plusDays(10).toString());
        when(restTemplate.getForEntity(contains("/senasa/validar-camion/"), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

        validacionService.verificarHabilitacionSenasa(hoy, camion);

        assertEquals(hoy.plusDays(10), camion.getVtoSenasa());
        verify(camionRepository, times(1)).save(camion);
    }

    @Test
    public void verificarHabilitacionSenasa_CuandoNoEstaVencida_NoDeberiaLlamarApi() {
        LocalDate hoy = LocalDate.now();
        Camion camion = new Camion();
        camion.setPatente("ABC-123");
        camion.setVtoSenasa(hoy.plusDays(5)); // Vence en 5 días (Vigente)

        validacionService.verificarHabilitacionSenasa(hoy, camion);

        // Verificamos que al estar vigente, NUNCA llame a la API ni guarde en BD
        verify(restTemplate, never()).getForEntity(anyString(), any());
        verify(camionRepository, never()).save(any());
    }

    @Test
    public void verificarHabilitacionSenasa_CuandoApiFalla_DeberiaLanzarExcepcion() {
        LocalDate hoy = LocalDate.now();
        Camion camion = new Camion();
        camion.setPatente("ABC-123");
        camion.setVtoSenasa(hoy.minusDays(1));

        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> 
            validacionService.verificarHabilitacionSenasa(hoy, camion)
        );
        assertTrue(ex.getMessage().contains("El camión no está habilitado"));
    }

    // ==========================================
    // TESTS PARA RUCA (Empresas/Establecimientos)
    // ==========================================

    @Test
    public void verificarRucaEmpresa_CuandoVencida_DeberiaLlamarApiYActualizar() {
        LocalDate hoy = LocalDate.now();
        EmpresaCliente empresa = new EmpresaCliente();
        empresa.setRucaNro("RUCA-999");
        empresa.setVtoRuca(hoy.minusDays(2)); // Vencida
        Establecimiento origen = new Establecimiento();
        origen.setEmpresa(empresa);

        Map<String, String> responseBody = Map.of("vtoRucaNuevo", hoy.plusYears(1).toString());
        when(restTemplate.getForEntity(contains("/ruca/validar-empresa/"), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

        validacionService.verificarRucaEmpresa(hoy, origen);

        assertEquals(hoy.plusYears(1), empresa.getVtoRuca());
        verify(empresaClienteRepository, times(1)).save(empresa);
    }

    @Test
    public void verificarRucaEmpresa_CuandoVigente_NoDeberiaLlamarApi() {
        LocalDate hoy = LocalDate.now();
        EmpresaCliente empresa = new EmpresaCliente();
        empresa.setVtoRuca(hoy.plusMonths(1)); // Vigente
        Establecimiento origen = new Establecimiento();
        origen.setEmpresa(empresa);

        validacionService.verificarRucaEmpresa(hoy, origen);

        verify(restTemplate, never()).getForEntity(anyString(), any());
        verify(empresaClienteRepository, never()).save(any());
    }

    // ==========================================
    // TESTS PARA CNRT (Choferes LINTI/Licencia)
    // ==========================================

    @Test
    public void verificarLicenciaChofer_CuandoVencida_DeberiaLlamarApiYActualizar() {
        LocalDate hoy = LocalDate.now();
        ChoferDetalle chofer = new ChoferDetalle();
        chofer.setNroLicencia("LIC-444");
        chofer.setVtoLicencia(hoy.minusDays(1)); // Vencida
        chofer.setVtoLinti(hoy.plusDays(10));    // Vigente (Pero al estar una vencida, debe chequear ambas)

        Map<String, String> responseBody = Map.of(
                "vtoLicenciaNuevo", hoy.plusYears(2).toString(),
                "vtoLintiNuevo", hoy.plusYears(1).toString()
        );
        
        when(restTemplate.getForEntity(contains("/cnrt/validar-chofer/"), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

        validacionService.verificarLicenciaChofer(hoy, chofer);

        assertEquals(hoy.plusYears(2), chofer.getVtoLicencia());
        assertEquals(hoy.plusYears(1), chofer.getVtoLinti());
        verify(choferDetalleRepository, times(1)).save(chofer);
    }

    @Test
    public void verificarLicenciaChofer_CuandoVigente_NoDeberiaLlamarApi() {
        LocalDate hoy = LocalDate.now();
        ChoferDetalle chofer = new ChoferDetalle();
        chofer.setVtoLicencia(hoy.plusMonths(5));
        chofer.setVtoLinti(hoy.plusMonths(5));

        validacionService.verificarLicenciaChofer(hoy, chofer);

        verify(restTemplate, never()).getForEntity(anyString(), any());
        verify(choferDetalleRepository, never()).save(any());
    }

    // ==========================================
    // TESTS PARA ARCA (Carta de Porte)
    // ==========================================

    @Test
    public void getNroAutorizacionArca_Exitoso_DeberiaRetornarNro() {
        String cpe = "12345678";
        Map<String, String> responseBody = Map.of("nroAutorizacion", "AUT-OK-2026");
        
        when(restTemplate.getForEntity(contains("/arca/validar-cpe/" + cpe), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

        String resultado = validacionService.getNroAutorizacionArca(cpe);

        assertEquals("AUT-OK-2026", resultado);
    }

    @Test
    public void getNroAutorizacionArca_FallaApi_DeberiaLanzarExcepcion() {
        String cpe = "CPE-FALSO";
        
        when(restTemplate.getForEntity(contains("/arca/validar-cpe/"), eq(Map.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> 
            validacionService.getNroAutorizacionArca(cpe)
        );
        assertTrue(ex.getMessage().contains("El CPE es inválido o está inactivo"));
    }
}