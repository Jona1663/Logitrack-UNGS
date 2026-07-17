package com.logitrack.sistema_logistica.service;

import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.logitrack.sistema_logistica.dto.UsuarioRequestDTO;
import com.logitrack.sistema_logistica.dto.UsuarioResponseDTO;
import com.logitrack.sistema_logistica.model.ChoferDetalle;
import com.logitrack.sistema_logistica.model.Persona;
import com.logitrack.sistema_logistica.model.Usuario;
import com.logitrack.sistema_logistica.model.enums.RolUsuario;
import com.logitrack.sistema_logistica.repository.ChoferDetalleRepository;
import com.logitrack.sistema_logistica.repository.PersonaRepository;
import com.logitrack.sistema_logistica.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final ChoferDetalleRepository choferDetalleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UsuarioResponseDTO crearUsuario(UsuarioRequestDTO request) {

        // agregamos validacion manual
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new RuntimeException("La contraseña es obligatoria para crear un nuevo usuario.");
        }

        if (usuarioRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Error: El nombre de usuario (email) ya está en uso.");
        }

        // guardamos usuario
        Usuario nuevoUsuario = Usuario.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .rol(request.getRol())
                .activo(true) // Por default lo creamos activo
                .build();

        Usuario usuarioGuardado = usuarioRepository.save(nuevoUsuario);

        // guardamos persona
        Persona nuevaPersona = Persona.builder()
                .idUsuario(usuarioGuardado) // Relacionamos con el Usuario recién guardado
                .cuil(request.getCuil())
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .telefono(request.getTelefono())
                .build();

        Persona personaGuardada = personaRepository.save(nuevaPersona);

        // si rol == chofer guardamos chofer
        if (request.getRol() == RolUsuario.CHOFER) {

            // Validamos que nos hayan mandado los datos del chofer
            if (request.getNroLicencia() == null || request.getVtoLicencia() == null || request.getVtoLinti() == null) {
                throw new RuntimeException("Error: Faltan datos de la licencia para dar de alta al Chofer.");
            }

            ChoferDetalle nuevoChofer = ChoferDetalle.builder()
                    .personaAsociada(personaGuardada) // Relacionamos con la Persona (@MapsId)
                    .nroLicencia(request.getNroLicencia())
                    .vtoLicencia(request.getVtoLicencia())
                    .vtoLinti(request.getVtoLinti())
                    .build();

            choferDetalleRepository.save(nuevoChofer);
        }

        // dto para el front
        return UsuarioResponseDTO.builder()
                .idUsuario(usuarioGuardado.getIdUsuario())
                .username(usuarioGuardado.getUsername())
                .rol(usuarioGuardado.getRol())
                .activo(usuarioGuardado.getActivo())
                .idPersona(personaGuardada.getIdPersona())
                .nombre(personaGuardada.getNombre())
                .apellido(personaGuardada.getApellido())
                .cuil(personaGuardada.getCuil())
                .telefono(personaGuardada.getTelefono())
                .build();
    }

    public List<UsuarioResponseDTO> listarUsuarios() {
        return usuarioRepository.findAll().stream()
                .map(u -> {
                    Persona p = personaRepository.findById(u.getIdUsuario()).orElse(new Persona());
                    return UsuarioResponseDTO.builder()
                            .idUsuario(u.getIdUsuario())
                            .username(u.getUsername())
                            .rol(u.getRol())
                            .activo(u.getActivo())
                            .nombre(p.getNombre())
                            .apellido(p.getApellido())
                            .cuil(p.getCuil())
                            .telefono(p.getTelefono())
                            .build();
                })
                .toList();
    }

    // no se elimina se setea activo en falso
    @Transactional
    public void deshabilitarUsuario(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public UsuarioResponseDTO actualizarUsuario(Integer id, UsuarioRequestDTO request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Persona persona = personaRepository.findByIdUsuario(usuario)
                .orElseThrow(() -> new RuntimeException("Datos personales no encontrados"));

        // Actualizamos datos de usuario
        usuario.setRol(request.getRol());
        usuarioRepository.save(usuario);

        // Actualizamos datos de persona
        persona.setNombre(request.getNombre());
        persona.setApellido(request.getApellido());
        persona.setTelefono(request.getTelefono());
        personaRepository.save(persona);

        // Retornamos el DTO actualizado
        return UsuarioResponseDTO.builder()
                .idUsuario(usuario.getIdUsuario())
                .username(usuario.getUsername())
                .rol(usuario.getRol())
                .activo(usuario.getActivo())
                .nombre(persona.getNombre())
                .apellido(persona.getApellido())
                .cuil(persona.getCuil())
                .telefono(persona.getTelefono())
                .build();
    }

    @Transactional
    public void resetearPassword(Integer id, String nuevaPassword) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(usuario);
    }

}