package com.logitrack.sistema_logistica.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import com.logitrack.sistema_logistica.model.EvaluacionPsicomotora;
import com.logitrack.sistema_logistica.dto.AlertaFatigaDTO;
import com.logitrack.sistema_logistica.dto.EvaluacionFatigaRequestDTO;
import com.logitrack.sistema_logistica.dto.EvaluacionFatigaResponseDTO;
import com.logitrack.sistema_logistica.model.ChoferDetalle;
import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.model.enums.EstadoEvaluacionEnum;
import com.logitrack.sistema_logistica.repository.ChoferDetalleRepository;
import com.logitrack.sistema_logistica.repository.EnvioRepository;
import com.logitrack.sistema_logistica.repository.EvaluacionPsicomotoraRepository;
import org.springframework.beans.factory.annotation.Value;
import java.time.LocalDateTime;

import org.springframework.transaction.annotation.Transactional;

@Service
public class EvaluacionFatigaService {
    @Autowired
    private EvaluacionPsicomotoraRepository repo;
    @Autowired
    private EnvioRepository envioRepository; // Necesario para buscar el envío
    @Autowired
    private ChoferDetalleRepository choferDetalleRepository; // Necesario para buscar el chofer
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private AlertaWebService alertaWebService;
    @Autowired
    private com.logitrack.sistema_logistica.repository.UsuarioRepository usuarioRepository;

    @Value("${logitrack.fatiga.umbral-ms}")
    private long umbralFatiga;

    @Transactional
    public EvaluacionFatigaResponseDTO procesarEvaluacion(EvaluacionFatigaRequestDTO dto, String username) {

        // Buscar las entidades reales a partir de los IDs proporcionados en el DTO
        Envio envio = envioRepository.findById(dto.getIdEnvio())
                .orElseThrow(() -> new RuntimeException("El envío no existe"));

        ChoferDetalle chofer = choferDetalleRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Chofer no encontrado"));

        // Mapear los datos al modelo
        EvaluacionPsicomotora eval = new EvaluacionPsicomotora();
        eval.setIdEnvio(envio);
        eval.setChoferId(chofer);
        eval.setTipoJuego(dto.getTipoJuego());
        eval.setTiempoReaccionMs(dto.getTiempoReaccionMs());
        eval.setFechaCreacion(LocalDateTime.now());

        // Hacemos save antes de disparar alertas
        EvaluacionPsicomotora evaluacionGuardada = repo.save(eval);

        // Lógica de Validación
        if (dto.getTiempoReaccionMs() < 100 || dto.getTiempoReaccionMs() > umbralFatiga) {
            eval.setResultado(EstadoEvaluacionEnum.RECHAZADO);
            eval.setEstadoBloqueo(EstadoEvaluacionEnum.RECHAZADO);

            // Creamos el DTO exacto que Jamil quiere reutilizar
            AlertaFatigaDTO alerta = new AlertaFatigaDTO(
                    envio.getIdEnvio(),
                    chofer.getPersonaAsociada().getNombre() + " " + chofer.getPersonaAsociada().getApellido(), // Mejor
                                                                                                               // enviar
                                                                                                               // el
                                                                                                               // nombre
                                                                                                               // real
                    "Fatiga detectada: Tiempo de reacción de " + dto.getTiempoReaccionMs() + "ms",
                    evaluacionGuardada.getId(),
                    evaluacionGuardada.getEstadoBloqueo());

            // Disparamos la alerta WebSocket
            messagingTemplate.convertAndSend("/topic/alertas-supervisores", alerta);

        } else {
            // Aprobó: Marcamos resultado APROBADO y el bloqueo como APROBADO (o LIBERADO)
            eval.setResultado(EstadoEvaluacionEnum.APROBADO);
            eval.setEstadoBloqueo(EstadoEvaluacionEnum.APROBADO);
            eval.setMensaje("Test superado correctamente.");

            // Disparar Alerta WebSocket al supervisor
            messagingTemplate.convertAndSend("/topic/alertas-supervisores",
                    "Alerta: Chofer " + username + " aprobó el test de fatiga en el envío " + dto.getIdEnvio());
        }

        // Guardar en base de datos
        repo.save(eval);

        // Retornar respuesta estructurada
        return new EvaluacionFatigaResponseDTO(
                eval.getId(),
                eval.getResultado() == EstadoEvaluacionEnum.APROBADO,
                eval.getMensaje());
    }

    @Transactional
    public void resetearEvaluacion(Long id) {
        EvaluacionPsicomotora eval = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Evaluación no encontrada"));

        // Cambiamos el estado a RESETEADO para que el chofer pueda volver a intentar
        eval.setEstadoBloqueo(EstadoEvaluacionEnum.RESETEADO);
        repo.save(eval);
    }

    @Transactional
    public void autorizarForzado(Long id, String motivo, String usernameSupervisor) {
        EvaluacionPsicomotora eval = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Evaluación no encontrada"));

        // Validamos que el motivo no sea nulo o vacío
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new RuntimeException("El motivo de autorización es obligatorio");
        }

        // Actualizamos estado y guardamos la justificación del supervisor
        eval.setEstadoBloqueo(EstadoEvaluacionEnum.OVERRIDE_AUTORIZADO);
        eval.setMotivoAutorizacion(motivo);
        eval.setAutorizadoPor(usernameSupervisor); // Guardamos quién fue

        repo.save(eval);
    }

    @Transactional
    public void rechazarPrueba(String username, String motivo, Long id) {
        EvaluacionPsicomotora eval = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Evaluación no encontrada"));

        // Validamos que el motivo no sea nulo o vacío
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new RuntimeException("El motivo de autorización es obligatorio");
        }

        eval.setEstadoBloqueo(EstadoEvaluacionEnum.RECHAZADO);
        eval.setMotivoAutorizacion(motivo);
        eval.setAutorizadoPor(username); // Guardamos quién fue

    }

    @Transactional(readOnly = true)
    public AlertaFatigaDTO obtenerEvaluacionPendienteParaEnvio(String idEnvio) {
        // Buscamos si hay alguna evaluación en estado RECHAZADO para este viaje
        return repo
                .findFirstByIdEnvio_IdEnvioAndEstadoBloqueoOrderByFechaCreacionDesc(idEnvio,
                        EstadoEvaluacionEnum.RECHAZADO)
                .map(eval -> {
                    // Si existe, armamos el DTO
                    String nombreCompleto = eval.getChoferId().getPersonaAsociada().getNombre() + " " +
                            eval.getChoferId().getPersonaAsociada().getApellido();

                    return new AlertaFatigaDTO(
                            eval.getIdEnvio().getIdEnvio(),
                            nombreCompleto,
                            "Fatiga detectada: Tiempo de reacción de " + eval.getTiempoReaccionMs() + "ms",
                            eval.getId(),
                            eval.getEstadoBloqueo());
                })
                // Si no hay nada rechazado, devolvemos null
                .orElse(null);
    }

}
