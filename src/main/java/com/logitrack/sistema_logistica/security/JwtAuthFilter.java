package com.logitrack.sistema_logistica.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.logitrack.sistema_logistica.repository.UsuarioRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Si no hay token, dejamos pasar (SecurityConfig decide si la ruta es pública o no)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            String username = jwtService.validateTokenAndGetUsername(token);

            usuarioRepository.findByUsername(username).ifPresentOrElse(usuario -> {
                if (Boolean.TRUE.equals(usuario.getActivo())) {
                    // Usuario activo: registramos la autenticación en el contexto
                    var auth = new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()))
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    // Usuario existe pero está inactivo: rechazamos explícitamente
                    try {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\": \"Usuario inactivo\"}");
                    } catch (IOException e) {
                        logger.error("Error escribiendo respuesta de usuario inactivo", e);
                    }
                }
            }, () -> {
                // El token era válido pero el usuario ya no existe en la BD
                try {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"Usuario no encontrado\"}");
                } catch (IOException e) {
                    logger.error("Error escribiendo respuesta de usuario no encontrado", e);
                }
            });

        } catch (JWTVerificationException e) {
            // Token malformado, expirado o con firma inválida
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Token inválido o expirado\"}");
            return; // Cortamos la cadena, no seguimos procesando
        }

        // Si la respuesta ya fue comprometida (usuario inactivo o no encontrado), no seguimos
        if (response.isCommitted()) {
            return;
        }

        filterChain.doFilter(request, response);
    }
}