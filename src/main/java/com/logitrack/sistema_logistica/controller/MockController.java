package com.logitrack.sistema_logistica.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/mock")
public class MockController {

    //mock ARCA Carta de porte electronica, si termina en 999 se rechaza

    @GetMapping("/arca/validar-cpe/{nroCpe}")
    public ResponseEntity<Map<String, Object>> validarCpe(@PathVariable String nroCpe) {
        Map<String, Object> response = new HashMap<>();
        
        response.put("nroCpeConsultado", nroCpe);
        
        if (nroCpe != null && nroCpe.endsWith("999")) {
            response.put("estado", "RECHAZADO");
            response.put("error", "La Carta de Porte no existe en los registros de ARCA o ha expirado.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        response.put("estado", "ACTIVO");
        response.put("nroAutorizacion", "AUTH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        return ResponseEntity.ok(response);
    }
    
     //SIMULADOR CNRT - Validación de Licencia LiNTI ,si la licencia contiene '999', el chofer está inhabilitado.
    
    @GetMapping("/cnrt/validar-chofer/{nroLicencia}")
    public ResponseEntity<Map<String, Object>> validarCnrt(@PathVariable String nroLicencia) {
        Map<String, Object> response = new HashMap<>();
        
        response.put("nroLicencia", nroLicencia);
        
        if (nroLicencia != null && nroLicencia.contains("999")) {
            response.put("estado", "INHABILITADO");
            response.put("motivo", "El chofer posee la licencia retenida o el psicofísico vencido.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        // si la licencia esta vencida pero el nro de licencia ds valido actualiza el vencimientos a un apño desde ñla fecha de chequeo
        String nuevoVencimiento = java.time.LocalDate.now().plusYears(1).toString();
        
        response.put("estado", "HABILITADO");
        response.put("vtoLicenciaNuevo", nuevoVencimiento);
        response.put("vtoLintiNuevo", nuevoVencimiento);
        
        return ResponseEntity.ok(response);
    }


     //SIMULADOR RUCA - Validación de Registro de Empresas Agroalimentarias, si el registro contiene 999, la empresa está suspendida.

    @GetMapping("/ruca/validar-empresa/{nroRuca}")
    public ResponseEntity<Map<String, Object>> validarRuca(@PathVariable String nroRuca) {
        Map<String, Object> response = new HashMap<>();
        
        response.put("rucaConsultado", nroRuca);

        if (nroRuca != null && nroRuca.contains("999")) {
            response.put("estado", "SUSPENDIDO");
            response.put("motivo", "Empresa inhabilitada para operar en el comercio de granos.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        // si el ruca esta vencido pero el nro de ruca ds valido actualiza el vencimientos a un apño desde ñla fecha de chequeo
        String nuevoVencimiento = java.time.LocalDate.now().plusYears(1).toString();

        response.put("estado", "ACTIVO");
        response.put("vtoRucaNuevo", nuevoVencimiento);
        
        return ResponseEntity.ok(response);
    }

    //Mock SENASA validacion de camiones si la patente tiene un 999 el camion esta inhabilitado
     @GetMapping("/senasa/validar-camion/{patente}")
    public ResponseEntity<Map<String, Object>> validarSenasa(@PathVariable String patente) {
        Map<String, Object> response = new HashMap<>();
        
        if (patente != null && patente.contains("999")) {
            response.put("habilitado", false);
            response.put("motivo", "Habilitación sanitaria vencida o inexistente para transporte de granos.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        String nuevoVencimiento = java.time.LocalDate.now().plusYears(1).toString();

        response.put("habilitado", true);
        response.put("vencimientoHabilitacion", nuevoVencimiento);
        return ResponseEntity.ok(response);
    }


}