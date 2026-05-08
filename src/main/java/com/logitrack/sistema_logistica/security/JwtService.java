package com.logitrack.sistema_logistica.security;

import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

@Service
public class JwtService {

    // La clave secreta se lee desde application.properties (nunca hardcodeada)
    @Value("${jwt.secret}")
    private String secret;

    // Genera un token JWT firmado con el username y rol del usuario
    public String generateToken(String username, String rol) {
        return JWT.create()
                .withSubject(username)           // quien es el usuario
                .withClaim("rol", rol)     // su rol (ADMINISTRADOR, CHOFER, etc.)
                .withIssuedAt(new Date())        // cuándo se emitió
                .sign(Algorithm.HMAC256(secret)); // firma con clave secreta
    }

    // Valida el token y devuelve el username si es válido
    public String validateTokenAndGetUsername(String token) throws JWTVerificationException {
        DecodedJWT decoded = JWT.require(Algorithm.HMAC256(secret))
                .build()
                .verify(token);
        return decoded.getSubject();
    }
}