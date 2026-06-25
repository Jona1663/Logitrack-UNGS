package com.logitrack.sistema_logistica.service;

import com.logitrack.sistema_logistica.model.AlertaWeb;
import com.logitrack.sistema_logistica.model.Usuario;
import com.logitrack.sistema_logistica.repository.AlertaWebRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AlertaWebService {

    @Autowired
    private AlertaWebRepository alertaWebRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void crearYEnviarAlerta(Usuario destino, String mensaje, String tipo, String idEnvio, Long idEvaluacion, String nombreChofer, String motivo) {
        // guardar en base de datos para el historial
        AlertaWeb alerta = AlertaWeb.builder()
                .usuario(destino)
                .mensaje(mensaje)
                .tipo(tipo)
                .idEnvio(idEnvio) 
                .idEvaluacion(idEvaluacion)
                .nombreChofer(nombreChofer)
                .motivo(motivo)
                .leido(false)
                .leido(false)
                .fechaHora(LocalDateTime.now())
                .build();

        alertaWebRepository.save(alerta);

        // enviar por websocket en tiempo real al canal del usuario
        String destinoCanal = "/queue/alertas-" + destino.getIdUsuario();
        messagingTemplate.convertAndSend(destinoCanal, alerta);
    }

    @Transactional
    public void crearYEnviarAlerta(Usuario destino, String mensaje, String tipo) {
        AlertaWeb alerta = AlertaWeb.builder()
                .usuario(destino)
                .mensaje(mensaje)
                .tipo(tipo)
                .leido(false)
                .fechaHora(LocalDateTime.now())
                .build();

        alertaWebRepository.save(alerta);

        // el frontend se suscribirá a /queue/alertas-{idUsuario}
        String destinoCanal = "/queue/alertas-" + destino.getIdUsuario();
        messagingTemplate.convertAndSend(destinoCanal, mensaje);
    }

    @Transactional(readOnly = true)
    public List<AlertaWeb> obtenerPendientes(Integer idUsuario) {
        return alertaWebRepository.findByUsuarioIdUsuarioAndLeidoFalseOrderByFechaHoraDesc(idUsuario);
    }

    @Transactional
    public void marcarComoLeida(Integer idAlertaWeb) {
        AlertaWeb alerta = alertaWebRepository.findById(idAlertaWeb)
                .orElseThrow(() -> new RuntimeException("alerta no encontrada"));
        alerta.setLeido(true);
        alertaWebRepository.save(alerta);
    }
}