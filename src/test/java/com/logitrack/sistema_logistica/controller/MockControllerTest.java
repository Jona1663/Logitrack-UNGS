package com.logitrack.sistema_logistica.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MockControllerTest {

    // Instanciamos el controlador directamente, sin magia de Spring
    private final MockController controller = new MockController();

    @Test
    public void validarCpe_conNumeroValido_retornaActivo() {
        ResponseEntity<Map<String, Object>> response = controller.validarCpe("123456");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("ACTIVO", response.getBody().get("estado"));
        assertNotNull(response.getBody().get("nroAutorizacion"));
    }

    @Test
    public void validarCpe_conNumeroInvalido_retornaRechazado() {
        ResponseEntity<Map<String, Object>> response = controller.validarCpe("123999");

        assertEquals(400, response.getStatusCode().value());
        assertEquals("RECHAZADO", response.getBody().get("estado"));
    }

    @Test
    public void validarCnrt_conLicenciaValida_retornaHabilitado() {
        ResponseEntity<Map<String, Object>> response = controller.validarCnrt("LIC-123");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("HABILITADO", response.getBody().get("estado"));
    }

    @Test
    public void validarCnrt_conLicenciaInvalida_retornaInhabilitado() {
        ResponseEntity<Map<String, Object>> response = controller.validarCnrt("LIC-999");

        assertEquals(403, response.getStatusCode().value());
        assertEquals("INHABILITADO", response.getBody().get("estado"));
    }

    @Test
    public void validarRuca_conRucaValido_retornaActivo() {
        ResponseEntity<Map<String, Object>> response = controller.validarRuca("RUCA-123");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("ACTIVO", response.getBody().get("estado"));
    }

    @Test
    public void validarRuca_conRucaInvalido_retornaSuspendido() {
        ResponseEntity<Map<String, Object>> response = controller.validarRuca("RUCA-999");

        assertEquals(403, response.getStatusCode().value());
        assertEquals("SUSPENDIDO", response.getBody().get("estado"));
    }

    @Test
    public void validarSenasa_conPatenteValida_retornaHabilitado() {
        ResponseEntity<Map<String, Object>> response = controller.validarSenasa("AAA123");

        assertEquals(200, response.getStatusCode().value());
        assertTrue((Boolean) response.getBody().get("habilitado"));
    }

    @Test
    public void validarSenasa_conPatenteInvalida_retornaInhabilitado() {
        ResponseEntity<Map<String, Object>> response = controller.validarSenasa("AAA999");

        assertEquals(403, response.getStatusCode().value());
        assertFalse((Boolean) response.getBody().get("habilitado"));
    }
}