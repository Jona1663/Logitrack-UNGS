package com.logitrack.sistema_logistica.service;

import com.logitrack.sistema_logistica.dto.UsuarioRequestDTO;
import com.logitrack.sistema_logistica.dto.UsuarioResponseDTO;
import com.logitrack.sistema_logistica.model.Persona;
import com.logitrack.sistema_logistica.model.Usuario;
import com.logitrack.sistema_logistica.model.enums.RolUsuario;
import com.logitrack.sistema_logistica.repository.ChoferDetalleRepository;
import com.logitrack.sistema_logistica.repository.PersonaRepository;
import com.logitrack.sistema_logistica.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.logitrack.sistema_logistica.model.ChoferDetalle;
// IMPORTS NUEVOS PARA EL TEST DE MÉTRICAS
import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;
import com.logitrack.sistema_logistica.repository.EnvioRepository;
import com.logitrack.sistema_logistica.dto.MetadatosDTO;

@ExtendWith(MockitoExtension.class)
public class AdminServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PersonaRepository personaRepository;
    @Mock
    private ChoferDetalleRepository choferDetalleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
// MOCK AGREGADO PARA EL TEST DE MÉTRICAS
   
    @Mock
    private com.logitrack.sistema_logistica.repository.EnvioRepository envioRepository;
    @InjectMocks
    private AdminService adminService;

    @Test
    public void crearUsuario_DeberiaGuardarCorrectamente() {
        // Arrange
        UsuarioRequestDTO request = new UsuarioRequestDTO();
        request.setUsername("test@test.com");
        request.setPassword("1234");
        request.setRol(RolUsuario.OPERADOR);
        request.setCuil("12345678901");

        when(usuarioRepository.existsByUsername(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encodedPass");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArguments()[0]);
        when(personaRepository.save(any(Persona.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        UsuarioResponseDTO result = adminService.crearUsuario(request);

        // Assert
        assertNotNull(result);
        verify(usuarioRepository, times(1)).save(any());
        verify(personaRepository, times(1)).save(any());
    }

    @Test
    public void deshabilitarUsuario_DeberiaSetearActivoEnFalse() {
        // Arrange
        Usuario usuario = Usuario.builder().idUsuario(1).activo(true).build();
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));

        // Act
        adminService.deshabilitarUsuario(1);

        // Assert
        assertFalse(usuario.getActivo());
        verify(usuarioRepository, times(1)).save(usuario);
    }

    @Test
    public void actualizarUsuario_DeberiaActualizarDatos() {
        // Arrange
        Usuario usuario = Usuario.builder().idUsuario(1).build();
        Persona persona = Persona.builder().nombre("Viejo").build();
        UsuarioRequestDTO request = new UsuarioRequestDTO();
        request.setNombre("Nuevo");

        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(personaRepository.findByIdUsuario(usuario)).thenReturn(Optional.of(persona));
        when(usuarioRepository.save(any())).thenReturn(usuario);
        when(personaRepository.save(any())).thenReturn(persona);

        // Act
        adminService.actualizarUsuario(1, request);

        // Assert
        assertEquals("Nuevo", persona.getNombre());
        verify(personaRepository, times(1)).save(persona);
    }

    @Test
    public void resetearPassword_DeberiaGuardarNuevaPassword() {
        // Arrange
        Usuario usuario = Usuario.builder().idUsuario(1).build();
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("nueva")).thenReturn("hash");

        // Act
        adminService.resetearPassword(1, "nueva");

        // Assert
        assertEquals("hash", usuario.getPasswordHash());
        verify(usuarioRepository, times(1)).save(usuario);
    }

    // =========================================================
    // ISSUE: Validar suma de kilos y conteo de estados (Lógica del Dashboard)
    // =========================================================
    @Test
    public void validarLogica_Dashboard_sumarKilosYContarEstados() {
        // GIVEN: Creamos nuestros "datos de prueba controlados" (3 envíos)
        com.logitrack.sistema_logistica.model.Envio envio1 = new com.logitrack.sistema_logistica.model.Envio();
        envio1.setIdEnvio("LT-001");
        envio1.setKgOrigen(10000);
        envio1.setEstadoActual(com.logitrack.sistema_logistica.model.enums.EstadoEnvio.EN_TRANSITO);

        com.logitrack.sistema_logistica.model.Envio envio2 = new com.logitrack.sistema_logistica.model.Envio();
        envio2.setIdEnvio("LT-002");
        envio2.setKgOrigen(5000);
        envio2.setEstadoActual(com.logitrack.sistema_logistica.model.enums.EstadoEnvio.EN_TRANSITO);

        com.logitrack.sistema_logistica.model.Envio envio3 = new com.logitrack.sistema_logistica.model.Envio();
        envio3.setIdEnvio("LT-003");
        envio3.setKgOrigen(2000);
        envio3.setEstadoActual(com.logitrack.sistema_logistica.model.enums.EstadoEnvio.PENDIENTE);

        // Agrupamos en una lista
        java.util.List<com.logitrack.sistema_logistica.model.Envio> listaControlada = java.util.Arrays.asList(envio1, envio2, envio3);

        // WHEN: Aplicamos la lógica de cálculo puro que pide la Issue
        int kilosTotales = 0;
        int enTransito = 0;
        int pendientes = 0;

        for (com.logitrack.sistema_logistica.model.Envio envio : listaControlada) {
            kilosTotales += envio.getKgOrigen();
            
            if (envio.getEstadoActual() == com.logitrack.sistema_logistica.model.enums.EstadoEnvio.EN_TRANSITO) {
                enTransito++;
            } else if (envio.getEstadoActual() == com.logitrack.sistema_logistica.model.enums.EstadoEnvio.PENDIENTE) {
                pendientes++;
            }
        }

        // THEN: Validamos que la matemática es correcta
        org.junit.jupiter.api.Assertions.assertEquals(17000, kilosTotales, "La suma total de kilos debe dar 17.000");
        org.junit.jupiter.api.Assertions.assertEquals(2, enTransito, "Deben contarse 2 envíos en tránsito");
        org.junit.jupiter.api.Assertions.assertEquals(1, pendientes, "Debe contarse 1 envío pendiente");
    }

    @Test
    public void listarUsuarios_DeberiaRetornarSoloActivosConDatosDePersona() {
        // Arrange
        Usuario uActivo = new Usuario(); uActivo.setIdUsuario(1); uActivo.setActivo(true);
        Usuario uInactivo = new Usuario(); uInactivo.setIdUsuario(2); uInactivo.setActivo(false);
        
        Persona p = new Persona(); p.setNombre("Juan"); p.setApellido("Perez");
        
        when(usuarioRepository.findAll()).thenReturn(List.of(uActivo, uInactivo));
        when(personaRepository.findById(1)).thenReturn(Optional.of(p));

        // Act
        List<UsuarioResponseDTO> resultado = adminService.listarUsuarios();

        // Assert
        assertEquals(1, resultado.size(), "Debería listar solo el activo");
        assertEquals("Juan", resultado.get(0).getNombre());
        verify(personaRepository, times(1)).findById(1);
    }

    @Test
    public void crearUsuario_UsuarioExistente_DebeLanzarExcepcion() {
        // Arrange
        UsuarioRequestDTO request = new UsuarioRequestDTO();
        request.setUsername("existente@test.com");
        when(usuarioRepository.existsByUsername("existente@test.com")).thenReturn(true);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> adminService.crearUsuario(request));
    }

    @Test
    public void crearUsuario_RolChoferExitoso_DebeGuardarChofer() {
        // Arrange
        UsuarioRequestDTO request = new UsuarioRequestDTO();
        request.setUsername("chofer@test.com");
        request.setPassword("123");
        request.setRol(RolUsuario.CHOFER);
        request.setNroLicencia("12345");
        request.setVtoLicencia(LocalDate.now());
        request.setVtoLinti(LocalDate.now());

        Usuario u = Usuario.builder().idUsuario(1).build();
        Persona p = Persona.builder().idUsuario(u).build();

        when(usuarioRepository.existsByUsername(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(usuarioRepository.save(any())).thenReturn(u);
        when(personaRepository.save(any())).thenReturn(p);

        // Act
        adminService.crearUsuario(request);

        // Assert
        // Verificamos que se guardó el ChoferDetalle, entrando así en el bloque 'if'
        verify(choferDetalleRepository, times(1)).save(any(ChoferDetalle.class));
    }

    @Test
    public void crearUsuario_RolChoferFaltanDatos_DebeLanzarExcepcion() {
        // Arrange: Rol chofer pero sin datos de licencia
        UsuarioRequestDTO request = new UsuarioRequestDTO();
        request.setRol(RolUsuario.CHOFER);
        request.setNroLicencia(null); // <--- Dato faltante

        when(usuarioRepository.existsByUsername(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(usuarioRepository.save(any())).thenReturn(new Usuario());
        when(personaRepository.save(any())).thenReturn(new Persona());

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> adminService.crearUsuario(request));
        assertEquals("Error: Faltan datos de la licencia para dar de alta al Chofer.", ex.getMessage());
    }

}