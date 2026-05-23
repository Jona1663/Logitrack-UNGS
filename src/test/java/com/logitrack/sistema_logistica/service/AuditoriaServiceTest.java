package com.logitrack.sistema_logistica.service;

import com.logitrack.sistema_logistica.dto.HistorialResponseDTO;
import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.model.HistorialEstados;
import com.logitrack.sistema_logistica.model.Usuario;
import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;
import com.logitrack.sistema_logistica.model.enums.TipoEvento;
import com.logitrack.sistema_logistica.repository.HistorialEstadosRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuditoriaServiceTest {

    @Mock
    private HistorialEstadosRepository historialEstadosRepository;

    @InjectMocks
    private AuditoriaService auditoriaService;

    // ==========================================
    // ESCENARIO 1: EL CAMINO FELIZ (Todo funciona)
    // ==========================================
    @Test
    public void registrarEvento_ConDatosValidos_DeberiaGuardarHistorial() {
        // Arrange
        Envio envio = Envio.builder().idEnvio("LT-123").build();
        Usuario usuario = new Usuario();
        usuario.setUsername("operador1");

        // Act
        auditoriaService.registrarEvento(
                envio,
                usuario,
                TipoEvento.CAMBIO_ESTADO,
                EstadoEnvio.PENDIENTE,
                EstadoEnvio.EN_TRANSITO
        );

        // Assert
        // Verificamos que el repositorio intentó guardar un objeto HistorialEstados con los datos correctos
        verify(historialEstadosRepository, times(1)).save(argThat(historial -> 
            historial.getEnvio().getIdEnvio().equals("LT-123") &&
            historial.getUsuario().getUsername().equals("operador1") &&
            historial.getTipoEvento() == TipoEvento.CAMBIO_ESTADO &&
            historial.getEstadoAnterior() == EstadoEnvio.PENDIENTE &&
            historial.getEstadoNuevo() == EstadoEnvio.EN_TRANSITO
        ));
    }

    @Test
    public void obtenerHistorialPorEnvio_ConResultados_DeberiaMapearADTO() {
        // Arrange
        String idEnvio = "LT-456";
        HistorialEstados registro1 = HistorialEstados.builder()
                .estadoNuevo(EstadoEnvio.PENDIENTE)
                .build();
        HistorialEstados registro2 = HistorialEstados.builder()
                .estadoNuevo(EstadoEnvio.EN_TRANSITO)
                .build();

        when(historialEstadosRepository.buscarHistorialPorEnvio(idEnvio))
                .thenReturn(List.of(registro1, registro2));

        // Act
        List<HistorialResponseDTO> resultado = auditoriaService.obtenerHistorialPorEnvio(idEnvio);

        // Assert
        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertEquals("PENDIENTE", resultado.get(0).getEstadoNuevo());
        assertEquals("EN_TRANSITO", resultado.get(1).getEstadoNuevo());
    }

    // ==========================================
    // ESCENARIO 2: EL CAMINO TRISTE (No hay datos)
    // ==========================================
    @Test
    public void obtenerHistorialPorEnvio_SinResultados_DeberiaRetornarListaVacia() {
        // Arrange
        String idEnvio = "LT-FANTASMA";
        
        // Simulamos que la base de datos devuelve una lista vacía
        when(historialEstadosRepository.buscarHistorialPorEnvio(idEnvio))
                .thenReturn(List.of());

        // Act
        List<HistorialResponseDTO> resultado = auditoriaService.obtenerHistorialPorEnvio(idEnvio);

        // Assert
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty(), "La lista debería estar vacía si no hay historial");
    }

    // ==========================================
    // ESCENARIO 3: EL DESASTRE (Falla la BD)
    // ==========================================
    @Test
    public void registrarEvento_CuandoFallaBaseDeDatos_DeberiaPropagarExcepcion() {
        // Arrange
        Envio envio = Envio.builder().idEnvio("LT-999").build();
        Usuario usuario = new Usuario();

        // Simulamos que al intentar guardar, la base de datos tira un error (ej. se cayó la conexión)
        when(historialEstadosRepository.save(any(HistorialEstados.class)))
                .thenThrow(new DataIntegrityViolationException("Error de constraint en BD"));

        // Act & Assert
        DataIntegrityViolationException ex = assertThrows(DataIntegrityViolationException.class, () -> 
            auditoriaService.registrarEvento(
                    envio,
                    usuario,
                    TipoEvento.CANCELACION,
                    EstadoEnvio.PENDIENTE,
                    EstadoEnvio.CANCELADO
            )
        );
        
        assertEquals("Error de constraint en BD", ex.getMessage());
    }
}