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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
}