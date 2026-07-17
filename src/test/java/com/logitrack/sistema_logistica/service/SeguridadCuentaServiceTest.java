package com.logitrack.sistema_logistica.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.logitrack.sistema_logistica.model.Usuario;
import com.logitrack.sistema_logistica.model.enums.RolUsuario;
import com.logitrack.sistema_logistica.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
public class SeguridadCuentaServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private NotificationService notificationService;
    @Mock private AlertaWebService alertaWebService;

    @InjectMocks
    private SeguridadCuentaService seguridadCuentaService;

    @Test
    public void manejarIntentoFallido_CuandoLlegaA5_DeberiaBloquear() {
        Usuario usuario = new Usuario();
        usuario.setUsername("test@mail.com");
        usuario.setIntentosFallidos(4); // Está en 4, pasará a 5

        when(usuarioRepository.findByUsername("test@mail.com")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findByRol(RolUsuario.SUPERVISOR)).thenReturn(List.of(new Usuario()));

        seguridadCuentaService.manejarIntentoFallido("test@mail.com");

        assertTrue(usuario.getBloqueado());
        assertNotNull(usuario.getCodigoDesbloqueo());
        verify(notificationService).enviarNotificacion(anyString(), anyString(), anyString());
    }

    @Test
    public void verificarCodigoDesbloqueo_CodigoValido_DeberiaDesbloquear() {
        Usuario usuario = new Usuario();
        usuario.setBloqueado(true);
        usuario.setCodigoDesbloqueo("123456");
        usuario.setVencimientoCodigo(LocalDateTime.now().plusMinutes(5));

        when(usuarioRepository.findByUsername("user")).thenReturn(Optional.of(usuario));

        boolean resultado = seguridadCuentaService.verificarCodigoDesbloqueo("user", "123456");

        assertTrue(resultado);
        assertFalse(usuario.getBloqueado());
        assertEquals(0, usuario.getIntentosFallidos());
    }

    @Test
    public void resetearIntentos_DeberiaPonerIntentosEnCero() {
        Usuario usuario = new Usuario();
        usuario.setIntentosFallidos(3);
        when(usuarioRepository.findByUsername("user")).thenReturn(Optional.of(usuario));

        seguridadCuentaService.resetearIntentos("user");

        assertEquals(0, usuario.getIntentosFallidos());
        verify(usuarioRepository).save(usuario);
    }
}