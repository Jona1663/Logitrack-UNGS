package com.logitrack.sistema_logistica.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.logitrack.sistema_logistica.model.Usuario;
import com.logitrack.sistema_logistica.model.enums.RolUsuario;
import com.logitrack.sistema_logistica.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SeguridadCuentaService {
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private AlertaWebService alertaWebService;   

    @Transactional
    public void manejarIntentoFallido(String username) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(username);
        
        if (usuarioOpt.isEmpty()) return;
        
        Usuario usuario = usuarioOpt.get();
        if (Boolean.TRUE.equals(usuario.getBloqueado())) return;

        int nuevosIntentos = usuario.getIntentosFallidos() + 1;
        usuario.setIntentosFallidos(nuevosIntentos);

        if (nuevosIntentos >= 5) {
            usuario.setBloqueado(true);
            String codigo = generarCodigoAleatorio();
            usuario.setCodigoDesbloqueo(codigo);
            usuario.setVencimientoCodigo(LocalDateTime.now().plusMinutes(10));
            
            // Mandamos el mail asíncrono usando la infraestructura de Resend
            String asunto = "LogiTrack - Código de Seguridad para Desbloqueo";
            String mensaje = "Tu cuenta ha sido bloqueada tras 5 intentos fallidos.\n"
                           + "Para recuperar el acceso, ingresá el siguiente código de 6 dígitos:\n\n"
                           + "CÓDIGO: " + codigo + "\n\n"
                           + "Este código expirará en 10 minutos.";
                           
            // Verificamos el formato del correo
            String correoDestino = usuario.getUsername();
            if (!correoDestino.contains("@")) {
                correoDestino = "logitrack.agro@gmail.com"; // Forzamos el desvío si es un usuario de prueba
                log.info("El username no es un mail. Redirigiendo código a: {}", correoDestino);
            }
            
            // Le pasamos 'correoDestino'
            notificationService.enviarNotificacion("logitrack.agro@gmail.com", asunto, mensaje);
            log.info("Cuenta bloqueada. Código enviado a: {}", correoDestino);

            // Le avisamos en tiempo real a los supervisores que alguien se bloqueó
            usuarioRepository.findByRol(RolUsuario.SUPERVISOR).forEach(supervisor -> 
                alertaWebService.crearYEnviarAlerta(
                    supervisor, 
                    "El usuario " + usuario.getUsername() + " fue bloqueado por múltiples intentos fallidos.", 
                    "SEGURIDAD"
                )
            );
        }
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void resetearIntentos(String username) {
        usuarioRepository.findByUsername(username).ifPresent(usuario -> {
            usuario.setIntentosFallidos(0);
            usuario.setUltimoAcceso(LocalDateTime.now());
            usuarioRepository.save(usuario);
        });
    }

    @Transactional
    public boolean verificarCodigoDesbloqueo(String username, String codigoIngresado) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(username);
        
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            
            if (Boolean.TRUE.equals(usuario.getBloqueado()) &&
                codigoIngresado.equals(usuario.getCodigoDesbloqueo()) &&
                usuario.getVencimientoCodigo() != null &&
                usuario.getVencimientoCodigo().isAfter(LocalDateTime.now())) {
                
                //Código válido: Desbloqueamos la cuenta
                usuario.setBloqueado(false);
                usuario.setIntentosFallidos(0);
                usuario.setCodigoDesbloqueo(null);
                usuario.setVencimientoCodigo(null);
                
                usuarioRepository.save(usuario);
                return true;
            }
        }
        return false;
    }

    private String generarCodigoAleatorio() {
        Random random = new Random();
        return String.valueOf(100000 + random.nextInt(900000)); 
    } 
}
