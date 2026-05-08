package com.logitrack.sistema_logistica.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logitrack.sistema_logistica.dto.LoginRequestDTO;
import com.logitrack.sistema_logistica.dto.LoginResponseDTO;
import com.logitrack.sistema_logistica.model.Usuario;
import com.logitrack.sistema_logistica.repository.UsuarioRepository;
import com.logitrack.sistema_logistica.security.JwtService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

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

        // Comparar la password ingresada con el hash guardado en BD
        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword_hash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Credenciales incorrectas");
        }

        //Generar y devolver el token JWT
        String token = jwtService.generateToken(
                usuario.getUsername(),
                usuario.getRol().name()
        );

        return ResponseEntity.ok(new LoginResponseDTO(
                token,
                usuario.getRol().name(),
                usuario.getUsername()
        ));
    }
}