package com.logitrack.sistema_logistica.config;

import com.logitrack.sistema_logistica.model.enums.RolUsuario;
import com.logitrack.sistema_logistica.model.enums.TipoEmpresa;
import com.logitrack.sistema_logistica.model.enums.TipoGrano;
import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;
import com.logitrack.sistema_logistica.model.*;
import com.logitrack.sistema_logistica.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
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
    private EmpresaClienteRepository empresaClienteRepository;
    @Autowired
    private EstablecimientoRepository establecimientoRepository;
    @Autowired
    private CamionRepository camionRepository;
    @Autowired
    private ChoferDetalleRepository choferDetalleRepository;
    @Autowired
    private EnvioRepository envioRepository;
    @Autowired
    private HistorialEstadosRepository historialEstadosRepository;

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
                    .passwordHash(passwordEncoder.encode("123456")).rol(RolUsuario.SUPERVISOR).activo(true).build());
            Usuario op = usuarioRepository.saveAndFlush(Usuario.builder().username("operador1")
                    .passwordHash(passwordEncoder.encode("123456")).rol(RolUsuario.OPERADOR).activo(true).build());
            Usuario ch1 = usuarioRepository.saveAndFlush(Usuario.builder().username("chofer1")
                    .passwordHash(passwordEncoder.encode("123456")).rol(RolUsuario.CHOFER).activo(true).build());
            Usuario ch2 = usuarioRepository.saveAndFlush(Usuario.builder().username("chofer2")
                    .passwordHash(passwordEncoder.encode("123456")).rol(RolUsuario.CHOFER).activo(true).build());
            Usuario ch3 = usuarioRepository.saveAndFlush(Usuario.builder().username("chofer3")
                    .passwordHash(passwordEncoder.encode("123456")).rol(RolUsuario.CHOFER).activo(true).build());
            Usuario ch4 = usuarioRepository.saveAndFlush(Usuario.builder().username("chofer4")
                    .passwordHash(passwordEncoder.encode("123456")).rol(RolUsuario.CHOFER).activo(true).build());

            // 2. Personas
            Persona p1 = personaRepository.saveAndFlush(Persona.builder().cuil("20-11111111-1").nombre("Laura")
                    .apellido("Gomez").idUsuario(admin).build());
            Persona p2 = personaRepository.saveAndFlush(Persona.builder().cuil("20-22222222-2").nombre("Martin")
                    .apellido("Rodriguez").idUsuario(op).build());
            Persona p3 = personaRepository.saveAndFlush(
                    Persona.builder().cuil("20-33333333-3").nombre("Juan").apellido("Perez").idUsuario(ch1).build());
            Persona p4 = personaRepository.saveAndFlush(
                    Persona.builder().cuil("20-44444444-4").nombre("Carlos").apellido("Lopez").idUsuario(ch2).build());
            Persona p5 = personaRepository.saveAndFlush(Persona.builder().cuil("20-55555555-5").nombre("Pedro")
                    .apellido("Alfonso").idUsuario(ch3).build());
            Persona p6 = personaRepository.saveAndFlush(
                    Persona.builder().cuil("20-66666666-6").nombre("Raul").apellido("Mesa").idUsuario(ch4).build());

            // 3. Choferes (Casos de prueba técnicos)
            ChoferDetalle cd1 = choferDetalleRepository.saveAndFlush(ChoferDetalle.builder().nroLicencia("LIC-100")
                    .vtoLicencia(futuro).vtoLinti(futuro).personaAsociada(p3).build());
            ChoferDetalle cd2 = choferDetalleRepository.saveAndFlush(ChoferDetalle.builder().nroLicencia("LIC-200")
                    .vtoLicencia(futuro).vtoLinti(futuro).personaAsociada(p4).build());
            // Chofer 3: Vencido pero VÁLIDO (Debería renovarse solo al crear un envío)
            ChoferDetalle cd3 = choferDetalleRepository.saveAndFlush(ChoferDetalle.builder()
                    .nroLicencia("LIC-OK-300").vtoLicencia(pasado).vtoLinti(pasado).personaAsociada(p5).build());
            // Chofer 4: Vencido e INVÁLIDO (Tiene 999, el Mock lo va a rebotar)
            ChoferDetalle cd4 = choferDetalleRepository.saveAndFlush(ChoferDetalle.builder()
                    .nroLicencia("LIC-999-BAD").vtoLicencia(pasado).vtoLinti(pasado).personaAsociada(p6).build());

            // 4. Empresas (4 Empresas)
            EmpresaCliente emp1 = empresaClienteRepository
                    .saveAndFlush(EmpresaCliente.builder().cuit("30-001").razonSocial("AgroExport S.A.")
                            .tipoEmpresa(TipoEmpresa.PUERTO).rucaNro("R-001").vtoRuca(futuro).build());
            EmpresaCliente emp2 = empresaClienteRepository
                    .saveAndFlush(EmpresaCliente.builder().cuit("30-002").razonSocial("Granos del Sur")
                            .tipoEmpresa(TipoEmpresa.ACOPIO).rucaNro("R-002").vtoRuca(futuro).build());
            EmpresaCliente emp3 = empresaClienteRepository
                    .saveAndFlush(EmpresaCliente.builder().cuit("30-003").razonSocial("Molinos Río")
                            .tipoEmpresa(TipoEmpresa.PRODUCTOR).rucaNro("R-003").vtoRuca(futuro).build());
            // Empresa 4: RUCA sospechoso (Para testear rechazo de empresa)
            EmpresaCliente emp4 = empresaClienteRepository
                    .saveAndFlush(EmpresaCliente.builder().cuit("30-999").razonSocial("Cargas Fantasma")
                            .tipoEmpresa(TipoEmpresa.ACOPIO).rucaNro("RUCA-999").vtoRuca(pasado).build());

            // 5. Establecimientos (3 por empresa = 12 total)
            List<Establecimiento> ests = new ArrayList<>();
            EmpresaCliente[] empresas = { emp1, emp2, emp3, emp4 };
            String[] nombres = { "Terminal A", "Silo Norte", "Planta Central" };
            for (int i = 0; i < empresas.length; i++) {
                for (int j = 0; j < 3; j++) {
                    ests.add(establecimientoRepository.saveAndFlush(Establecimiento.builder()
                            .nombreLugar(nombres[j] + " - " + empresas[i].getRazonSocial())
                            .direccion("Direccion " + i + j)
                            .empresa(empresas[i])
                            .latitud(-34.0 + (i * 0.1)).longitud(-58.0 + (j * 0.1)).build()));
                }
            }

            // 6. Camiones (Mantenemos los 2 y agregamos 2 más)
            Camion cam1 = camionRepository.saveAndFlush(Camion.builder().patente("AE123XX").rutaNro("RUTA-1")
                    .taraVacioKg(8500).vtoSenasa(futuro).build());
            Camion cam2 = camionRepository.saveAndFlush(Camion.builder().patente("AD456YY").rutaNro("RUTA-2")
                    .taraVacioKg(8200).vtoSenasa(futuro).build());
            // Camion 3: Vencido pero renovable
            Camion cam3 = camionRepository.saveAndFlush(Camion.builder().patente("AF789ZZ").rutaNro("RUTA-3")
                    .taraVacioKg(9000).vtoSenasa(pasado).build());
            // Camion 4: INHABILITADO (999 en patente)
            Camion cam4 = camionRepository.saveAndFlush(Camion.builder().patente("BAD-999").rutaNro("RUTA-4")
                    .taraVacioKg(8800).vtoSenasa(pasado).build());

            // 7. Envíos (10 Envíos con variedad de datos)
            for (int i = 1; i <= 10; i++) {
                Envio env = Envio.builder()
                        .cpe("CPE-00" + i)
                        .autorizacionARCA("AUTH-PROV-" + i)
                        .origen(ests.get(i % 12))
                        .destino(ests.get(0))
                        .chofer(i % 2 == 0 ? cd1 : cd2) // Usamos los choferes que están al día para que no fallen al
                                                        // arrancar
                        .camion(i % 2 == 0 ? cam1 : cam2)
                        .tipoGrano(i % 2 == 0 ? TipoGrano.SOJA : TipoGrano.MAIZ)
                        .estadoActual(EstadoEnvio.PENDIENTE)
                        .prioridadIa(i < 5 ? "ALTA" : "MEDIA")
                        .kgOrigen(25000 + (i * 500))
                        .distanciaKm(100.0 + (i * 10))
                        .build();

                env = envioRepository.saveAndFlush(env);
                historialEstadosRepository.saveAndFlush(HistorialEstados.builder().envio(env).usuario(op)
                        .estadoNuevo(EstadoEnvio.PENDIENTE).build());
            }

            System.out.println("✅ DATOS SEMILLA CARGADOS EXITOSAMENTE");

        } catch (Exception e) {
            throw new RuntimeException("❌ ERROR EN DATA SEED: " + e.getMessage(), e);
        }
    }
}
