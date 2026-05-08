package com.logitrack.sistema_logistica.config;

import com.logitrack.sistema_logistica.model.enums.Rol_Usuario;
import com.logitrack.sistema_logistica.model.enums.Tipo_Empresa;
import com.logitrack.sistema_logistica.model.enums.Tipo_Grano;
import com.logitrack.sistema_logistica.model.enums.Estado_Envio;
import com.logitrack.sistema_logistica.model.*;
import com.logitrack.sistema_logistica.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.Arrays;
import org.springframework.security.crypto.password.PasswordEncoder;

@Component
public class DataSeed implements CommandLineRunner {

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PersonaRepository personaRepository;
    @Autowired
    private Empresa_ClienteRepository empresaClienteRepository;
    @Autowired
    private EstablecimientoRepository establecimientoRepository;
    @Autowired
    private CamionRepository camionRepository;
    @Autowired
    private Chofer_DetalleRepository choferDetalleRepository;
    @Autowired
    private EnvioRepository envioRepository;
    @Autowired
    private Historial_EstadosRepository historialEstadosRepository;
        
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Solo insertamos datos si la tabla de usuarios está vacía
        if (usuarioRepository.count() == 0) {
            cargarDatosSemilla();
            System.out.println("🌱 DATOS SEMILLA CARGADOS CON ÉXITO 🌱");
        } else {
            System.out.println("✅ La base de datos ya contiene información. Se omite el DataSeed.");
        }
    }


   @Transactional
    protected void cargarDatosSemilla() {
        try {
            // 1. Usuarios
           Usuario admin = Usuario.builder().username("supervisor1").password_hash(passwordEncoder.encode("123456")).rol(Rol_Usuario.SUPERVISOR).activo(true).build();
            Usuario op = Usuario.builder().username("operador1").password_hash(passwordEncoder.encode("123456")).rol(Rol_Usuario.OPERADOR).activo(true).build();
            Usuario chofer1 = Usuario.builder().username("chofer1").password_hash(passwordEncoder.encode("123456")).rol(Rol_Usuario.CHOFER).activo(true).build();
            Usuario chofer2 = Usuario.builder().username("chofer2").password_hash(passwordEncoder.encode("123456")).rol(Rol_Usuario.CHOFER).activo(true).build();
            
            admin = usuarioRepository.saveAndFlush(admin);
            op = usuarioRepository.saveAndFlush(op);
            chofer1 = usuarioRepository.saveAndFlush(chofer1);
            chofer2 = usuarioRepository.saveAndFlush(chofer2);

            // 2. Personas
            Persona p1 = Persona.builder().cuil("20-11111111-1").nombre("Laura").apellido("Gomez").telefono("1122334455").id_usuario(admin).build();
            Persona p2 = Persona.builder().cuil("20-22222222-2").nombre("Martin").apellido("Rodriguez").telefono("1133445566").id_usuario(op).build();
            Persona p3 = Persona.builder().cuil("20-33333333-3").nombre("Juan").apellido("Perez").telefono("1144556677").id_usuario(chofer1).build();
            Persona p4 = Persona.builder().cuil("20-44444444-4").nombre("Carlos").apellido("Lopez").telefono("1155667788").id_usuario(chofer2).build();
            
            p1 = personaRepository.saveAndFlush(p1);
            p2 = personaRepository.saveAndFlush(p2);
            p3 = personaRepository.saveAndFlush(p3);
            p4 = personaRepository.saveAndFlush(p4);

            // 3. Choferes
            // ACA ESTA EL CAMBIO CLAVE: Usamos el ID directamente del objeto guardado y forzamos la asignacion
            LocalDate vencimiento = LocalDate.now().plusYears(1);
            Chofer_Detalle cd1 = Chofer_Detalle.builder().nro_licencia("LIC-001").vto_licencia(vencimiento).vto_linti(vencimiento).persona_asociada(p3).build();
            Chofer_Detalle cd2 = Chofer_Detalle.builder().nro_licencia("LIC-002").vto_licencia(vencimiento).vto_linti(vencimiento).persona_asociada(p4).build();
            choferDetalleRepository.saveAll(Arrays.asList(cd1, cd2));
            // 4. Empresas y Establecimientos
            Empresa_Cliente emp1 = Empresa_Cliente.builder().cuit("30-11111111-2").razon_social("Cargill").tipo_empresa(Tipo_Empresa.PUERTO).ruca_nro("R-001").ruca_estado(true).build();
            Empresa_Cliente emp2 = Empresa_Cliente.builder().cuit("30-22222222-3").razon_social("Los Grobo").tipo_empresa(Tipo_Empresa.ACOPIO).ruca_nro("R-002").ruca_estado(true).build();
            
            emp1 = empresaClienteRepository.saveAndFlush(emp1);
            emp2 = empresaClienteRepository.saveAndFlush(emp2);

            Establecimiento est1 = Establecimiento.builder().nombre_lugar("Puerto 1").direccion("Rosario").empresa(emp1).latitud(80.00).longitud(90.00).build();
            Establecimiento est2 = Establecimiento.builder().nombre_lugar("Acopio 2").direccion("Pergamino").empresa(emp2).latitud(85.00).longitud(95.00).build();
            
            est1 = establecimientoRepository.saveAndFlush(est1);
            est2 = establecimientoRepository.saveAndFlush(est2);

            // 5. Camiones
            Camion cam1 = Camion.builder().patente("AE123XX").ruta_nro("Ruta 9").tara_vacio_kg(8500).vto_senasa(vencimiento).build();
            Camion cam2 = Camion.builder().patente("AD456YY").ruta_nro("Ruta 8").tara_vacio_kg(8200).vto_senasa(vencimiento).build();
            
            cam1 = camionRepository.saveAndFlush(cam1);
            cam2 = camionRepository.saveAndFlush(cam2);

            // 6. Envíos
            Envio env1 = Envio.builder().cpe("CPE-001").origen(est2).destino(est1).chofer(cd1).camion(cam1).tipo_grano(Tipo_Grano.SOJA).estado_actual(Estado_Envio.PENDIENTE).prioridad_ia("ALTA").kg_origen(30000).distancia_km(80.00).build();
            Envio env2 = Envio.builder().cpe("CPE-002").origen(est2).destino(est1).chofer(cd2).camion(cam2).tipo_grano(Tipo_Grano.MAIZ).estado_actual(Estado_Envio.PENDIENTE).prioridad_ia("MEDIA").kg_origen(28000).distancia_km(120.00).build();
            
            env1 = envioRepository.saveAndFlush(env1);
            env2 = envioRepository.saveAndFlush(env2);

            // 7. Historial
            Historial_Estados he1 = Historial_Estados.builder().envio(env1).usuario(op).estado_nuevo(Estado_Envio.PENDIENTE).build();
            Historial_Estados he2 = Historial_Estados.builder().envio(env2).usuario(op).estado_nuevo(Estado_Envio.PENDIENTE).build();
            
            historialEstadosRepository.saveAndFlush(he1);
            historialEstadosRepository.saveAndFlush(he2);
            
        } catch (Exception e) {
            System.err.println("❌ ERROR AL CARGAR DATOS SEMILLA: " + e.getMessage());
            // Si algo falla, forzamos la excepción para que el Rollback sí funcione.
            throw new RuntimeException(e);
        }
    }}

    