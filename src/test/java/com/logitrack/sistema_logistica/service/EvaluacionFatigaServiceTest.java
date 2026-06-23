package com.logitrack.sistema_logistica.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.logitrack.sistema_logistica.dto.EvaluacionFatigaRequestDTO;
import com.logitrack.sistema_logistica.dto.EvaluacionFatigaResponseDTO;
import com.logitrack.sistema_logistica.model.*;
import com.logitrack.sistema_logistica.model.enums.EstadoEvaluacionEnum;
import com.logitrack.sistema_logistica.repository.*;

@ExtendWith(MockitoExtension.class)
public class EvaluacionFatigaServiceTest {

    @Mock private EvaluacionPsicomotoraRepository repo;
    @Mock private EnvioRepository envioRepository;
    @Mock private ChoferDetalleRepository choferDetalleRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private EvaluacionFatigaService service;

   @Test
    public void procesarEvaluacion_FatigaDetectada_DeberiaRechazar() {
        // 1. Armamos la cadena completa de objetos necesarios
        Usuario usuario = new Usuario();
        usuario.setUsername("chofer1");
        
        Persona persona = new Persona();
        persona.setIdUsuario(usuario); // Acá está el que te faltaba
        
        ChoferDetalle chofer = new ChoferDetalle();
        chofer.setPersonaAsociada(persona); // Y acá el padre de la persona

        EvaluacionFatigaRequestDTO dto = new EvaluacionFatigaRequestDTO();
        dto.setIdEnvio("LT-1");
        dto.setTiempoReaccionMs(5000L);

        // 2. Configuramos los mocks
        when(envioRepository.findById("LT-1")).thenReturn(Optional.of(new Envio()));
        when(choferDetalleRepository.findByUsername("chofer1")).thenReturn(Optional.of(chofer));
        when(repo.save(any(EvaluacionPsicomotora.class))).thenAnswer(i -> i.getArguments()[0]);

        // 3. Ejecutamos
        EvaluacionFatigaResponseDTO res = service.procesarEvaluacion(dto, "chofer1");

        // 4. Assert
        assertFalse(res.isAprobado());
        verify(messagingTemplate).convertAndSend(eq("/topic/alertas-supervisores"), any(Object.class));
    }

    @Test
    public void autorizarForzado_DeberiaCambiarEstado() {
        EvaluacionPsicomotora eval = new EvaluacionPsicomotora();
        when(repo.findById(1L)).thenReturn(Optional.of(eval));

        service.autorizarForzado(1L, "Fuerza mayor", "super1");

        assertEquals(EstadoEvaluacionEnum.OVERRIDE_AUTORIZADO, eval.getEstadoBloqueo());
        verify(repo).save(eval);
    }

    @Test
    public void resetearEvaluacion_DeberiaResetear() {
        EvaluacionPsicomotora eval = new EvaluacionPsicomotora();
        when(repo.findById(1L)).thenReturn(Optional.of(eval));

        service.resetearEvaluacion(1L);

        assertEquals(EstadoEvaluacionEnum.RESETEADO, eval.getEstadoBloqueo());
        verify(repo).save(eval);
    }
}