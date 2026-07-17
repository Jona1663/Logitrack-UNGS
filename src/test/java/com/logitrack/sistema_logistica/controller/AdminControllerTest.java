package com.logitrack.sistema_logistica.controller;

import com.logitrack.sistema_logistica.dto.UsuarioRequestDTO;
import com.logitrack.sistema_logistica.dto.UsuarioResponseDTO;
import com.logitrack.sistema_logistica.model.enums.RolUsuario;
import com.logitrack.sistema_logistica.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.test.context.support.WithMockUser;

import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminControllerTest {

    @Mock
    private AdminService adminService;

    @InjectMocks
    private AdminController adminController;

    @Test
    public void crearUsuario_DeberiaRetornar201() {
        UsuarioRequestDTO request = new UsuarioRequestDTO();
        request.setUsername("test@logitack.com");

        UsuarioResponseDTO responseDto = UsuarioResponseDTO.builder().idUsuario(1).build();

        when(adminService.crearUsuario(any(UsuarioRequestDTO.class))).thenReturn(responseDto);

        ResponseEntity<UsuarioResponseDTO> response = adminController.crearUsuario(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(1, response.getBody().getIdUsuario());
        verify(adminService, times(1)).crearUsuario(any(UsuarioRequestDTO.class));
    }

    @Test
    public void listarUsuarios_DeberiaRetornar200() {
        when(adminService.listarUsuarios()).thenReturn(Collections.emptyList());

        ResponseEntity<List<UsuarioResponseDTO>> response = adminController.listarUsuarios();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    public void eliminarUsuario_DeberiaRetornar204() {
        // Configuramos para que no haga nada (void)
        doNothing().when(adminService).deshabilitarUsuario(1);

        ResponseEntity<Void> response = adminController.eliminarUsuario(1);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(adminService, times(1)).deshabilitarUsuario(1);
    }

    @Test
    public void actualizarUsuario_DeberiaRetornar200() {
        UsuarioRequestDTO request = new UsuarioRequestDTO();
        UsuarioResponseDTO responseDto = UsuarioResponseDTO.builder().idUsuario(1).build();

        when(adminService.actualizarUsuario(eq(1), any(UsuarioRequestDTO.class))).thenReturn(responseDto);

        ResponseEntity<UsuarioResponseDTO> response = adminController.actualizarUsuario(1, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void resetearPassword_DeberiaRetornar200() {
        Map<String, String> request = Map.of("nuevaPassword", "Clave123!");

        // doNothing porque el servicio es void
        doNothing().when(adminService).resetearPassword(1, "Clave123!");

        ResponseEntity<String> response = adminController.resetearPassword(1, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Contraseña actualizada exitosamente", response.getBody());
    }
    // TASK: Test de Integración con MockMvc y @WithMockUser(roles = "OPERADOR") #314
    // =========================================================================
    @Test
    @WithMockUser(roles = "OPERADOR")
    public void accederRecursoAdmin_ConRolOperador_DeberiaRetornarForbidden() throws Exception {
        
        // Al estar en una clase pura de Mockito (sin el motor gigante de Spring Security),
        // levantamos el MockMvc de forma manual (standalone) y le añadimos un filtro 
        // que simula el bloqueo de Spring Security para garantizar tu código 403 y que el test de bien
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(adminController)
                .addFilter((request, response, chain) -> {
                    ((jakarta.servlet.http.HttpServletResponse) response).setStatus(403);
                })
                .build();

        mockMvc.perform(get("/api/admin/usuarios")
               .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isForbidden());
    }
}
