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
import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;
import com.logitrack.sistema_logistica.model.enums.EstadoEvaluacionEnum;
import com.logitrack.sistema_logistica.repository.ChoferDetalleRepository;
import com.logitrack.sistema_logistica.repository.EnvioRepository;
import com.logitrack.sistema_logistica.repository.EvaluacionPsicomotoraRepository;
import org.springframework.beans.factory.annotation.Value;
import com.logitrack.sistema_logistica.model.Usuario;
import com.logitrack.sistema_logistica.model.enums.RolUsuario;
import java.util.List;

import java.time.LocalDateTime;
import java.util.Map;

import jakarta.transaction.Transactional;

@Service
public class EvaluacionFatigaService {
    @Autowired private EvaluacionPsicomotoraRepository repo;
    @Autowired private EnvioRepository envioRepository; // Necesario para buscar el envío
    @Autowired private ChoferDetalleRepository choferDetalleRepository; // Necesario para buscar el chofer
    @Autowired private SimpMessagingTemplate messagingTemplate;
    @Autowired private AlertaWebService alertaWebService;
    @Autowired private com.logitrack.sistema_logistica.repository.UsuarioRepository usuarioRepository;

    @Value("${logitrack.fatiga.umbral-ms}")
    private long umbralFatiga;

    @Transactional
    public EvaluacionFatigaResponseDTO procesarEvaluacion(EvaluacionFatigaRequestDTO dto, String username) {
        
        // 1. Buscar las entidades reales a partir de los IDs proporcionados en el DTO
        Envio envio = envioRepository.findById(dto.getIdEnvio())
                .orElseThrow(() -> new RuntimeException("El envío no existe"));
        
        ChoferDetalle chofer = choferDetalleRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Chofer no encontrado"));

        // 2. Mapear los datos al modelo
        EvaluacionPsicomotora eval = new EvaluacionPsicomotora();
        eval.setIdEnvio(envio);
        eval.setChoferId(chofer);
        eval.setTipoJuego(dto.getTipoJuego());
        eval.setTiempoReaccionMs(dto.getTiempoReaccionMs());
        eval.setFechaCreacion(LocalDateTime.now());

        // --- HACEMOS EL SAVE ANTES DE DISPARAR ALERTAS ---
        EvaluacionPsicomotora evaluacionGuardada = repo.save(eval);

        //  Version 1
        /*
        // 3. Lógica de Validación (Criterio 2 y 3)
        if (dto.getTiempoReaccionMs() < 100 || dto.getTiempoReaccionMs() > umbralFatiga) {
            // Falló: Marcamos resultado RECHAZADO y el bloqueo queda ACTIVO
            eval.setResultado(EstadoEvaluacionEnum.RECHAZADO);
            eval.setEstadoBloqueo(EstadoEvaluacionEnum.RECHAZADO);
            //eval.setMensaje("Test no superado: Fatiga detectada o error de ejecución.");

            // Nueva lógica de WebSocket:
            AlertaFatigaDTO alerta = new AlertaFatigaDTO(
                envio.getIdEnvio(),
                chofer.getPersonaAsociada().getIdUsuario().getUsername(), // O el nombre que tengas en la entidad Chofer/Persona
                "Fatiga detectada: Tiempo de reacción de " + dto.getTiempoReaccionMs() + "ms",
                evaluacionGuardada.getId()
            );

            //System.out.println("DEBUG WEBSOCKET: Enviando alerta con ID EVALUACION: " + alerta.getIdEvaluacion());

            // Alerta WebSocket
            //System.out.println("DEBUG: Disparando alerta WebSocket al canal /topic/alertas para el chofer: " + username);
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
        */

        //version 2
        /* 
        if (dto.getTiempoReaccionMs() < 100 || dto.getTiempoReaccionMs() > umbralFatiga) {
            // Falló: Marcamos resultado RECHAZADO y el bloqueo queda ACTIVO
            eval.setResultado(EstadoEvaluacionEnum.RECHAZADO);
            eval.setEstadoBloqueo(EstadoEvaluacionEnum.RECHAZADO);
            //eval.setMensaje("Test no superado: Fatiga detectada o error de ejecución.");

            // Nueva lógica de WebSocket:
            AlertaFatigaDTO alerta = new AlertaFatigaDTO(
                envio.getIdEnvio(),
                chofer.getPersonaAsociada().getIdUsuario().getUsername(), // O el nombre que tengas en la entidad Chofer/Persona
                "Fatiga detectada: Tiempo de reacción de " + dto.getTiempoReaccionMs() + "ms",
                evaluacionGuardada.getId()
            );

            //System.out.println("DEBUG WEBSOCKET: Enviando alerta con ID EVALUACION: " + alerta.getIdEvaluacion());

            // Alerta WebSocket
            //System.out.println("DEBUG: Disparando alerta WebSocket al canal /topic/alertas para el chofer: " + username);
            messagingTemplate.convertAndSend("/topic/alertas-supervisores", alerta);

            // 2. --- NUEVA LÓGICA DE PERSISTENCIA (Para solucionar el F5) ---
            try {
                // Buscamos a todos los supervisores
                List<com.logitrack.sistema_logistica.model.Usuario> supervisores = 
                    usuarioRepository.findByRol(com.logitrack.sistema_logistica.model.enums.RolUsuario.SUPERVISOR);

                String mensajeVisual = "ALERTA DE FATIGA: El chofer " + chofer.getPersonaAsociada().getIdUsuario().getUsername() 
                                     + " registró " + dto.getTiempoReaccionMs() + "ms en viaje " + envio.getIdEnvio();

                // Guardamos en BD para cada supervisor usando tu nuevo método que acepta idEnvio
                for (com.logitrack.sistema_logistica.model.Usuario supervisor : supervisores) {
                    if (supervisor.getActivo()) {
                        alertaWebService.crearYEnviarAlerta(supervisor, mensajeVisual, "FATIGA", envio.getIdEnvio());
                    }
                }
            } catch (Exception e) {
                System.err.println("Error al guardar la alerta de fatiga en BD: " + e.getMessage());
            }
            
        } 
        */
       //Version 3
       if (dto.getTiempoReaccionMs() < 100 || dto.getTiempoReaccionMs() > umbralFatiga) {
            // Falló: Marcamos resultado RECHAZADO y el bloqueo queda ACTIVO
            eval.setResultado(EstadoEvaluacionEnum.RECHAZADO);
            eval.setEstadoBloqueo(EstadoEvaluacionEnum.RECHAZADO);

            // --- ELIMINAMOS LA DOBLE NOTIFICACIÓN ---
            // Borramos la creación de AlertaFatigaDTO y el messagingTemplate flotante de acá arriba.
            // Ahora centralizamos todo en la persistencia de la base de datos.

            try {
                // Buscamos a todos los supervisores
                List<com.logitrack.sistema_logistica.model.Usuario> supervisores = 
                    usuarioRepository.findByRol(com.logitrack.sistema_logistica.model.enums.RolUsuario.SUPERVISOR);

                String mensajeVisual = "ALERTA DE FATIGA: El chofer " + chofer.getPersonaAsociada().getIdUsuario().getUsername() 
                                     + " registró " + dto.getTiempoReaccionMs() + "ms en viaje " + envio.getIdEnvio();

                String nombreCompletoChofer = chofer.getPersonaAsociada().getNombre() + " " + chofer.getPersonaAsociada().getApellido();
                String motivoDetalle = "Fatiga detectada: Tiempo de reacción de " + dto.getTiempoReaccionMs() + "ms";

                // Guardamos en BD para cada supervisor pasando la estructura completa
                for (com.logitrack.sistema_logistica.model.Usuario supervisor : supervisores) {
                    if (supervisor.getActivo()) {
                        // Pasamos los 7 parámetros requeridos para salvar el F5
                        alertaWebService.crearYEnviarAlerta(
                            supervisor, 
                            mensajeVisual, 
                            "FATIGA", 
                            envio.getIdEnvio(),
                            evaluacionGuardada.getId(), // idEvaluacion crucial para los botones
                            nombreCompletoChofer,
                            motivoDetalle
                        );
                    }
                }
            } catch (Exception e) {
                System.err.println("Error al guardar la alerta de fatiga en BD: " + e.getMessage());
            }
        }else {
            // Aprobó: Marcamos resultado APROBADO y el bloqueo como APROBADO (o LIBERADO)
            eval.setResultado(EstadoEvaluacionEnum.APROBADO);
            eval.setEstadoBloqueo(EstadoEvaluacionEnum.APROBADO);
            eval.setMensaje("Test superado correctamente.");
            
            // Disparar Alerta WebSocket al supervisor
            messagingTemplate.convertAndSend("/topic/alertas-supervisores", 
                "Alerta: Chofer " + username + " aprobó el test de fatiga en el envío " + dto.getIdEnvio());
        } 


        // 4. Guardar en base de datos
        repo.save(eval);

        // 5. Retornar respuesta estructurada
        return new EvaluacionFatigaResponseDTO(
            eval.getId(),
            eval.getResultado() == EstadoEvaluacionEnum.APROBADO, 
            eval.getMensaje()
        );
    }

    // Lógica para la Tarea #600 - Resetear test de fatiga
    @Transactional
    public void resetearEvaluacion(Long id) {
        EvaluacionPsicomotora eval = repo.findById(id)
            .orElseThrow(() -> new RuntimeException("Evaluación no encontrada"));
        
        // Cambiamos el estado a RESETEADO para que el chofer pueda volver a intentar
        eval.setEstadoBloqueo(EstadoEvaluacionEnum.RESETEADO);
        repo.save(eval);
    }

    // Lógica para la Tarea #600 - Autorización por fuerza mayor
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
        public void rechazarPrueba(String username, String motivo, Long id ){
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


}
