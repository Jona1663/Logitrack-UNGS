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
import java.util.List;
import java.util.ArrayList;

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
            LocalDate futuro = LocalDate.now().plusYears(1);
            LocalDate pasado = LocalDate.now().minusMonths(2);

            // 1. Usuarios (Mantenemos los originales)
            Usuario admin = usuarioRepository.saveAndFlush(Usuario.builder().username("supervisor1")
                    .password_hash(passwordEncoder.encode("123456")).rol(Rol_Usuario.SUPERVISOR).activo(true).build());
            Usuario op = usuarioRepository.saveAndFlush(Usuario.builder().username("operador1")
                    .password_hash(passwordEncoder.encode("123456")).rol(Rol_Usuario.OPERADOR).activo(true).build());
            Usuario ch1 = usuarioRepository.saveAndFlush(Usuario.builder().username("chofer1")
                    .password_hash(passwordEncoder.encode("123456")).rol(Rol_Usuario.CHOFER).activo(true).build());
            Usuario ch2 = usuarioRepository.saveAndFlush(Usuario.builder().username("chofer2")
                    .password_hash(passwordEncoder.encode("123456")).rol(Rol_Usuario.CHOFER).activo(true).build());
            Usuario ch3 = usuarioRepository.saveAndFlush(Usuario.builder().username("chofer3")
                    .password_hash(passwordEncoder.encode("123456")).rol(Rol_Usuario.CHOFER).activo(true).build());
            Usuario ch4 = usuarioRepository.saveAndFlush(Usuario.builder().username("chofer4")
                    .password_hash(passwordEncoder.encode("123456")).rol(Rol_Usuario.CHOFER).activo(true).build());

            // 2. Personas
            Persona p1 = personaRepository.saveAndFlush(Persona.builder().cuil("20-11111111-1").nombre("Laura")
                    .apellido("Gomez").id_usuario(admin).build());
            Persona p2 = personaRepository.saveAndFlush(Persona.builder().cuil("20-22222222-2").nombre("Martin")
                    .apellido("Rodriguez").id_usuario(op).build());
            Persona p3 = personaRepository.saveAndFlush(
                    Persona.builder().cuil("20-33333333-3").nombre("Juan").apellido("Perez").id_usuario(ch1).build());
            Persona p4 = personaRepository.saveAndFlush(
                    Persona.builder().cuil("20-44444444-4").nombre("Carlos").apellido("Lopez").id_usuario(ch2).build());
            Persona p5 = personaRepository.saveAndFlush(Persona.builder().cuil("20-55555555-5").nombre("Pedro")
                    .apellido("Alfonso").id_usuario(ch3).build());
            Persona p6 = personaRepository.saveAndFlush(
                    Persona.builder().cuil("20-66666666-6").nombre("Raul").apellido("Mesa").id_usuario(ch4).build());

            // 3. Choferes (Casos de prueba técnicos)
            Chofer_Detalle cd1 = choferDetalleRepository.saveAndFlush(Chofer_Detalle.builder().nro_licencia("LIC-100")
                    .vto_licencia(futuro).vto_linti(futuro).persona_asociada(p3).build());
            Chofer_Detalle cd2 = choferDetalleRepository.saveAndFlush(Chofer_Detalle.builder().nro_licencia("LIC-200")
                    .vto_licencia(futuro).vto_linti(futuro).persona_asociada(p4).build());
            // Chofer 3: Vencido pero VÁLIDO (Debería renovarse solo al crear un envío)
            Chofer_Detalle cd3 = choferDetalleRepository.saveAndFlush(Chofer_Detalle.builder()
                    .nro_licencia("LIC-OK-300").vto_licencia(pasado).vto_linti(pasado).persona_asociada(p5).build());
            // Chofer 4: Vencido e INVÁLIDO (Tiene 999, el Mock lo va a rebotar)
            Chofer_Detalle cd4 = choferDetalleRepository.saveAndFlush(Chofer_Detalle.builder()
                    .nro_licencia("LIC-999-BAD").vto_licencia(pasado).vto_linti(pasado).persona_asociada(p6).build());

            // 4. Empresas (4 Empresas)
            Empresa_Cliente emp1 = empresaClienteRepository
                    .saveAndFlush(Empresa_Cliente.builder().cuit("30-001").razon_social("AgroExport S.A.")
                            .tipo_empresa(Tipo_Empresa.PUERTO).ruca_nro("R-001").vto_ruca(futuro).build());
            Empresa_Cliente emp2 = empresaClienteRepository
                    .saveAndFlush(Empresa_Cliente.builder().cuit("30-002").razon_social("Granos del Sur")
                            .tipo_empresa(Tipo_Empresa.ACOPIO).ruca_nro("R-002").vto_ruca(futuro).build());
            Empresa_Cliente emp3 = empresaClienteRepository
                    .saveAndFlush(Empresa_Cliente.builder().cuit("30-003").razon_social("Molinos Río")
                            .tipo_empresa(Tipo_Empresa.PRODUCTOR).ruca_nro("R-003").vto_ruca(futuro).build());
            // Empresa 4: RUCA sospechoso (Para testear rechazo de empresa)
            Empresa_Cliente emp4 = empresaClienteRepository
                    .saveAndFlush(Empresa_Cliente.builder().cuit("30-999").razon_social("Cargas Fantasma")
                            .tipo_empresa(Tipo_Empresa.ACOPIO).ruca_nro("RUCA-999").vto_ruca(pasado).build());

            // 5. Establecimientos (3 por empresa = 12 total)
            List<Establecimiento> ests = new ArrayList<>();
            Empresa_Cliente[] empresas = { emp1, emp2, emp3, emp4 };
            String[] nombres = { "Terminal A", "Silo Norte", "Planta Central" };
            for (int i = 0; i < empresas.length; i++) {
                for (int j = 0; j < 3; j++) {
                    ests.add(establecimientoRepository.saveAndFlush(Establecimiento.builder()
                            .nombre_lugar(nombres[j] + " - " + empresas[i].getRazon_social())
                            .direccion("Direccion " + i + j)
                            .empresa(empresas[i])
                            .latitud(-34.0 + (i * 0.1)).longitud(-58.0 + (j * 0.1)).build()));
                }
            }

            // 6. Camiones (Mantenemos los 2 y agregamos 2 más)
            Camion cam1 = camionRepository.saveAndFlush(Camion.builder().patente("AE123XX").ruta_nro("RUTA-1")
                    .tara_vacio_kg(8500).vto_senasa(futuro).build());
            Camion cam2 = camionRepository.saveAndFlush(Camion.builder().patente("AD456YY").ruta_nro("RUTA-2")
                    .tara_vacio_kg(8200).vto_senasa(futuro).build());
            // Camion 3: Vencido pero renovable
            Camion cam3 = camionRepository.saveAndFlush(Camion.builder().patente("AF789ZZ").ruta_nro("RUTA-3")
                    .tara_vacio_kg(9000).vto_senasa(pasado).build());
            // Camion 4: INHABILITADO (999 en patente)
            Camion cam4 = camionRepository.saveAndFlush(Camion.builder().patente("BAD-999").ruta_nro("RUTA-4")
                    .tara_vacio_kg(8800).vto_senasa(pasado).build());

            // 7. Envíos (10 Envíos con variedad de datos)
            for (int i = 1; i <= 10; i++) {
                Envio env = Envio.builder()
                        .cpe("CPE-00" + i)
                        .autorizacion_ARCA("AUTH-PROV-" + i)
                        .origen(ests.get(i % 12))
                        .destino(ests.get(0))
                        .chofer(i % 2 == 0 ? cd1 : cd2) // Usamos los choferes que están al día para que no fallen al
                                                        // arrancar
                        .camion(i % 2 == 0 ? cam1 : cam2)
                        .tipo_grano(i % 2 == 0 ? Tipo_Grano.SOJA : Tipo_Grano.MAIZ)
                        .estado_actual(Estado_Envio.PENDIENTE)
                        .prioridad_ia(i < 5 ? "ALTA" : "MEDIA")
                        .kg_origen(25000 + (i * 500))
                        .distancia_km(100.0 + (i * 10))
                        .build();

                env = envioRepository.saveAndFlush(env);
                historialEstadosRepository.saveAndFlush(Historial_Estados.builder().envio(env).usuario(op)
                        .estado_nuevo(Estado_Envio.PENDIENTE).build());
            }

            System.out.println("✅ DATOS SEMILLA CARGADOS EXITOSAMENTE");

        } catch (Exception e) {
            throw new RuntimeException("❌ ERROR EN DATA SEED: " + e.getMessage(), e);
        }
    }
}
