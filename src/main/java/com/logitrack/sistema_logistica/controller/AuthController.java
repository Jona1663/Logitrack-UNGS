package com.logitrack.sistema_logistica.controller;

import com.logitrack.sistema_logistica.dto.DesbloqueoCuentaRequestDTO;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.logitrack.sistema_logistica.repository.UsuarioRepository;
import com.logitrack.sistema_logistica.service.SeguridadCuentaService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.logitrack.sistema_logistica.dto.LoginResponseDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.logitrack.sistema_logistica.dto.LoginRequestDTO;
import com.logitrack.sistema_logistica.security.JwtService;
import com.logitrack.sistema_logistica.model.Usuario;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

        private final UsuarioRepository usuarioRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;
        private final SeguridadCuentaService seguridadService; // Para manejar bloqueos y alertas

        @PostMapping("/login")
        public ResponseEntity<?> login(@RequestBody LoginRequestDTO request) {

                // Buscar usuario por username
                Usuario usuario = usuarioRepository.findByUsername(request.getUsername())
                                .orElse(null);

                // Si no existe o está inactivo → 401
                if (usuario == null || !Boolean.TRUE.equals(usuario.getActivo())) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                        .body("Usuario no encontrado o inactivo");
                }

                // logica de bloqueo
                if (Boolean.TRUE.equals(usuario.getBloqueado())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                        .body(Map.of("error",
                                                        "Cuenta bloqueada por seguridad. Revisa tu correo electrónico para ingresar el código de recuperación."));
                }

                // Comparar la password ingresada con el hash guardado en BD
                if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {

                        // Contraseña mal: Sumamos intento fallido en la base de datos
                        seguridadService.manejarIntentoFallido(usuario.getUsername());

                        // Volvemos a buscar al usuario para tener sus datos frescos
                        usuario = usuarioRepository.findByUsername(usuario.getUsername()).get();

                        // Ahora sí, con los datos actualizados, preguntamos si se bloqueó
                        if (Boolean.TRUE.equals(usuario.getBloqueado())) {
                                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                                .body(Map.of("error",
                                                                "Cuenta bloqueada por seguridad. Revisa tu correo electrónico para ingresar el código de recuperación."));
                        }

                        // Calculamos cuántos le quedan para mostrarle un mensaje amigable
                        int restantes = 5 - (usuario.getIntentosFallidos());
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                        .body(Map.of("error", "Credenciales incorrectas. Te quedan " + restantes
                                                        + " intentos."));
                }

                // Si entra bien, le reiniciamos los contadores a 0
                seguridadService.resetearIntentos(usuario.getUsername());

                // Generar y devolver el token JWT
                String token = jwtService.generateToken(
                                usuario.getUsername(),
                                usuario.getRol().name());

                return ResponseEntity.ok(new LoginResponseDTO(
                                usuario.getIdUsuario(),
                                token,
                                usuario.getRol().name(),
                                usuario.getUsername()));
        }

        // NUEVO ENDPOINT PARA RECUPERAR LA CUENTA
        @PostMapping("/desbloquear")
        public ResponseEntity<?> desbloquearCuenta(@RequestBody DesbloqueoCuentaRequestDTO request) {
                boolean exito = seguridadService.verificarCodigoDesbloqueo(request.getUsername(), request.getCodigo());

                if (exito) {
                        return ResponseEntity.ok(
                                        Map.of("mensaje", "Cuenta desbloqueada con éxito. Ya puedes iniciar sesión."));
                } else {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(Map.of("error", "Código inválido o expirado."));
                }
        }
}