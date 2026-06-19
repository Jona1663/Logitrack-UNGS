package com.logitrack.sistema_logistica.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.logitrack.sistema_logistica.model.Camion;
import com.logitrack.sistema_logistica.model.ChoferDetalle;
import com.logitrack.sistema_logistica.model.EmpresaCliente;
import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.model.Establecimiento;
import com.logitrack.sistema_logistica.model.EvaluacionPsicomotora;
import com.logitrack.sistema_logistica.model.HistorialEstados;
import com.logitrack.sistema_logistica.model.Persona;
import com.logitrack.sistema_logistica.model.Usuario;
import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;
import com.logitrack.sistema_logistica.model.enums.EstadoEvaluacionEnum;
import com.logitrack.sistema_logistica.model.enums.RolUsuario;
import com.logitrack.sistema_logistica.model.enums.TipoEmpresa;
import com.logitrack.sistema_logistica.model.enums.TipoEvento;
import com.logitrack.sistema_logistica.model.enums.TipoGrano;
import com.logitrack.sistema_logistica.repository.CamionRepository;
import com.logitrack.sistema_logistica.repository.ChoferDetalleRepository;
import com.logitrack.sistema_logistica.repository.EmpresaClienteRepository;
import com.logitrack.sistema_logistica.repository.EnvioRepository;
import com.logitrack.sistema_logistica.repository.EstablecimientoRepository;
import com.logitrack.sistema_logistica.repository.EvaluacionPsicomotoraRepository;
import com.logitrack.sistema_logistica.repository.HistorialEstadosRepository;
import com.logitrack.sistema_logistica.repository.PersonaRepository;
import com.logitrack.sistema_logistica.repository.UsuarioRepository;
import java.util.Random;
import org.springframework.beans.factory.annotation.Value;

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
        @Autowired
        private EvaluacionPsicomotoraRepository evaluacionRepository;

        @Value("${logitrack.fatiga.umbral-ms}")
        private long umbralFatiga;

        @Override
        public void run(String... args) throws Exception {
                // Solo insertamos datos si la tabla de usuarios está vacía
                if (usuarioRepository.count() == 0) {
                        cargarDatosSemilla();
                        System.out.println("DATOS SEMILLA CARGADOS CON ÉXITO ");
                } else {
                        System.out.println("La base de datos ya contiene información. Se omite el DataSeed.");
                }
        }

    @Transactional
    protected void cargarDatosSemilla() {
        try {
LocalDate futuro = LocalDate.now().plusYears(1);
            LocalDate pasado = LocalDate.now().minusMonths(1);
            String defaultPass = passwordEncoder.encode("123456");

            // =========================================================================
            // 1. ADMINISTRADORES (2)
            // =========================================================================
            Usuario adm1 = usuarioRepository.saveAndFlush(Usuario.builder().username("admin1@logitrack.com").passwordHash(defaultPass).rol(RolUsuario.ADMINISTRADOR).activo(true).build());
            personaRepository.saveAndFlush(Persona.builder().cuil("20-10000001-1").nombre("Carlos").apellido("Admin").telefono("1122334455").idUsuario(adm1).build());

            Usuario adm2 = usuarioRepository.saveAndFlush(Usuario.builder().username("admin2@logitrack.com").passwordHash(defaultPass).rol(RolUsuario.ADMINISTRADOR).activo(true).build());
            personaRepository.saveAndFlush(Persona.builder().cuil("20-10000002-2").nombre("Lucia").apellido("Jefa").telefono("1122334456").idUsuario(adm2).build());

            // =========================================================================
            // 2. OPERADORES (2)
            // =========================================================================
            Usuario op1 = usuarioRepository.saveAndFlush(Usuario.builder().username("operador1@logitrack.com").passwordHash(defaultPass).rol(RolUsuario.OPERADOR).activo(true).build());
            personaRepository.saveAndFlush(Persona.builder().cuil("20-20000001-1").nombre("Marcos").apellido("Operario").telefono("1133445566").idUsuario(op1).build());

            Usuario op2 = usuarioRepository.saveAndFlush(Usuario.builder().username("operador2@logitrack.com").passwordHash(defaultPass).rol(RolUsuario.OPERADOR).activo(true).build());
            personaRepository.saveAndFlush(Persona.builder().cuil("20-20000002-2").nombre("Ana").apellido("Recepcion").telefono("1133445567").idUsuario(op2).build());

            // =========================================================================
            // 3. SUPERVISORES (2)
            // =========================================================================
            Usuario sup1 = usuarioRepository.saveAndFlush(Usuario.builder().username("supervisor1@logitrack.com").passwordHash(defaultPass).rol(RolUsuario.SUPERVISOR).activo(true).build());
            personaRepository.saveAndFlush(Persona.builder().cuil("20-30000001-1").nombre("Roberto").apellido("Control").telefono("1144556677").idUsuario(sup1).build());

            Usuario sup2 = usuarioRepository.saveAndFlush(Usuario.builder().username("supervisor2@logitrack.com").passwordHash(defaultPass).rol(RolUsuario.SUPERVISOR).activo(true).build());
            personaRepository.saveAndFlush(Persona.builder().cuil("20-30000002-2").nombre("Elena").apellido("Supervisora").telefono("1144556678").idUsuario(sup2).build());

            // =========================================================================
            // 4. CHOFERES (10)
            // =========================================================================
            List<ChoferDetalle> choferes = new ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                // CASOS TRAMPA PARA TESTING:
                // El chofer 9 tiene la licencia/LINTI vencida.
                // El chofer 10 está inactivo/no disponible (ej: de vacaciones).
                boolean activoYDisponible = (i != 10); 
                LocalDate fechaVtoChofer = (i == 9) ? pasado : futuro;

                Usuario ch = usuarioRepository.saveAndFlush(Usuario.builder()
                        .username("chofer" + i + "@logitrack.com")
                        .passwordHash(defaultPass)
                        .rol(RolUsuario.CHOFER)
                        .activo(activoYDisponible)
                        .build());

                // Formateamos el CUIL para que sea único (ej: 20-40000001-9)
                String cuilFormateado = "20-400000" + String.format("%02d", i) + "-9";

                Persona pCh = personaRepository.saveAndFlush(Persona.builder()
                        .cuil(cuilFormateado)
                        .nombre("NombreChofer" + i)
                        .apellido("ApellidoChofer" + i)
                        .telefono("11556677" + String.format("%02d", i))
                        .idUsuario(ch)
                        .build());

                ChoferDetalle det = choferDetalleRepository.saveAndFlush(ChoferDetalle.builder()
                        .personaAsociada(pCh)
                        .nroLicencia("LIC-CH-" + 1000 + i)
                        .vtoLicencia(fechaVtoChofer)
                        .vtoLinti(fechaVtoChofer)
                        .disponible(activoYDisponible)
                        .build());
                
                choferes.add(det);
            }

            // =========================================================================
            // 5. CAMIONES (10)
            // =========================================================================
            List<Camion> camiones = new ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                // CASOS TRAMPA PARA TESTING:
                // El camión 9 tiene el Senasa vencido.
                // El camión 10 está en el taller (no disponible).
                LocalDate fechaVtoCamion = (i == 9) ? pasado : futuro;
                boolean disponible = (i != 10);

                Camion cam = camionRepository.saveAndFlush(Camion.builder()
                        .patente("AE" + 100 + i + "XX") // Patentes tipo Mercosur: AE101XX
                        .rutaNro("RUTA-" + i)
                        .vtoSenasa(fechaVtoCamion)
                        .capacidadCargaKg(28000 + (i * 1000)) // Varían entre 29.000 y 38.000 kg
                        .disponible(disponible)
                        .build());
                
                camiones.add(cam);
            }

            // --- EMPRESAS (4 en total) ---
            EmpresaCliente emp1 = empresaClienteRepository.saveAndFlush(EmpresaCliente.builder()
                    .cuit("30-70111222-1").razonSocial("Agropecuaria Las Pampas S.A.")
                    .tipoEmpresa(TipoEmpresa.PRODUCTOR).rucaNro("R-1001").vtoRuca(futuro).email("contacto@laspampas.com.ar").build());

            EmpresaCliente emp2 = empresaClienteRepository.saveAndFlush(EmpresaCliente.builder()
                    .cuit("30-70333444-2").razonSocial("Acopios del Centro S.R.L.")
                    .tipoEmpresa(TipoEmpresa.ACOPIO).rucaNro("R-2002").vtoRuca(futuro).email("logistica@acopioscentro.com.ar").build());

            EmpresaCliente emp3 = empresaClienteRepository.saveAndFlush(EmpresaCliente.builder()
                    .cuit("30-70555666-3").razonSocial("Terminal Portuaria Rosario")
                    .tipoEmpresa(TipoEmpresa.PUERTO).rucaNro("R-3003").vtoRuca(futuro).email("operaciones@tprosario.com.ar").build());

            // CASO TRAMPA: Empresa con RUCA 999 y vencido (Para probar validaciones)
            EmpresaCliente emp4 = empresaClienteRepository.saveAndFlush(EmpresaCliente.builder()
                    .cuit("30-70999999-9").razonSocial("Molino Fantasma S.A.")
                    .tipoEmpresa(TipoEmpresa.MOLINO).rucaNro("RUCA-999").vtoRuca(pasado).email("admin@molinofantasma.com.ar").build());

            // --- ESTABLECIMIENTOS (5 por cada empresa = 20 en total) ---
            List<Establecimiento> establecimientos = new ArrayList<>();
            
            // Establecimientos Empresa 1 (Productor - Prov. Bs As y La Pampa)
            establecimientos.add(crearEstablecimiento("Estancia El Sol", "Ruta 8 Km 120, San Antonio de Areco", -34.2500, -59.4667, emp1));
            establecimientos.add(crearEstablecimiento("Campo La Esperanza", "Camino Vecinal S/N, Pergamino", -34.1833, -60.5833, emp1));
            establecimientos.add(crearEstablecimiento("Lote Los Ombúes", "Ruta 5 Km 250, 9 de Julio", -33.8167, -60.8833, emp1));
            establecimientos.add(crearEstablecimiento("Chacra San José", "Ruta 33 Km 320, Trenque Lauquen", -35.9667, -62.7333, emp1));
            establecimientos.add(crearEstablecimiento("Estancia Sur", "Ruta 5 Km 600, Santa Rosa", -36.6167, -64.2833, emp1));

            // Establecimientos Empresa 2 (Acopio - Sur de Santa Fe y Córdoba)
            establecimientos.add(crearEstablecimiento("Planta Silos Venado Tuerto", "Ruta 8 y Ruta 33, Venado Tuerto", -33.7500, -61.9667, emp2));
            establecimientos.add(crearEstablecimiento("Acopio Rufino", "Ruta 33 Km 535, Rufino", -34.2667, -62.7167, emp2));
            establecimientos.add(crearEstablecimiento("Planta Casilda", "Ruta 33 Km 740, Casilda", -33.0333, -61.1667, emp2));
            establecimientos.add(crearEstablecimiento("Silos Rio Cuarto", "Ruta 8 Km 600, Río Cuarto", -33.1333, -64.3500, emp2));
            establecimientos.add(crearEstablecimiento("Acopio Marcos Juárez", "Ruta 9 Km 430, Marcos Juárez", -32.7000, -62.1000, emp2));

            // Establecimientos Empresa 3 (Puertos - Zona Gran Rosario, Bahía Blanca, Quequén)
            establecimientos.add(crearEstablecimiento("Terminal San Lorenzo", "Ruta 11 Km 330, San Lorenzo", -32.7333, -60.7333, emp3));
            establecimientos.add(crearEstablecimiento("Puerto Gral. San Martín", "Av. San Martín S/N, Puerto Gral. San Martín", -32.7167, -60.7333, emp3));
            establecimientos.add(crearEstablecimiento("Terminal Timbúes", "Ruta 11 Km 345, Timbúes", -32.6833, -60.7500, emp3));
            establecimientos.add(crearEstablecimiento("Puerto Ing. White", "Zona Portuaria, Bahía Blanca", -38.7833, -62.2667, emp3));
            establecimientos.add(crearEstablecimiento("Puerto Quequén", "Av. 10 y Calle 59, Necochea", -38.5667, -58.7000, emp3));

            // Establecimientos Empresa 4 (Molinos - Dispersos / Casos Trampa)
            establecimientos.add(crearEstablecimiento("Molino Norte", "Ruta 9 Km 200, Ramallo", -33.4833, -60.0000, emp4));
            establecimientos.add(crearEstablecimiento("Planta Harinera Centro", "Parque Industrial, Pilar", -33.4500, -59.9000, emp4));
            establecimientos.add(crearEstablecimiento("Molino Sur", "Ruta 3 Km 150, Las Flores", -36.0167, -59.1000, emp4));
            establecimientos.add(crearEstablecimiento("Depósito 999", "Dirección Inexistente 999, CABA", -34.6037, -58.3816, emp4)); // CASO TRAMPA
            establecimientos.add(crearEstablecimiento("Molino Viejo", "Ruta 188 Km 100, Rojas", -33.9500, -60.7333, emp4));

            LocalDateTime hoyExacto = LocalDateTime.now();

            for (int i = 1; i <= 50; i++) {
                // 1. Asignaciones seguras: Usamos los primeros 8 choferes y camiones (sanos y habilitados)
                ChoferDetalle choferAsignado = choferes.get(i % 8);
                Camion camionAsignado = camiones.get(i % 8);
                
                // 2. Origen y destino cruzados para que nunca sean el mismo establecimiento
                Establecimiento origen = establecimientos.get(i % 20);
                Establecimiento destino = establecimientos.get((i + 7) % 20);
                
                TipoGrano[] todosLosGranos = TipoGrano.values();
                TipoGrano grano = todosLosGranos[i % todosLosGranos.length];

                // 3. Fechas dinámicas esparcidas en los últimos 2 meses
                LocalDateTime fechaCreacion = hoyExacto.minusDays(60 - i).minusHours(i % 10);
                LocalDateTime fechaSalida = fechaCreacion.plusHours(3);
                LocalDateTime fechaEta = fechaSalida.plusHours(12 + (i % 8)); // Tardan entre 12 y 19 horas
                LocalDateTime fechaLlegadaReal;
                
                EstadoEnvio estado;
                String comentarios;
                Integer kgOrigen = 30000 + (i * 50);
                Integer kgDestino = null;

                // 4. Casuística de los viajes
                if (i <= 5) {
                    // --> 5 CANCELADOS (Los primeros 5 del loop)
                    estado = EstadoEnvio.CANCELADO;
                    fechaSalida = null;
                    fechaLlegadaReal = null;
                    comentarios = "Cancelado por falta de cupo en puerto de destino o rotura antes de salir.";
                } else if (i <= 20) {
                    // --> 15 ENTREGADOS PERO LLEGADA TARDE (Retrasos)
                    estado = EstadoEnvio.ENTREGADO;
                    fechaLlegadaReal = fechaEta.plusHours(2 + (i % 5)); // Llegó entre 2 y 6 horas tarde
                    kgDestino = kgOrigen - 150; // Merma normal en viaje largo
                    comentarios = "Entregado fuera de término. Demoras por tráfico pesado y controles en ruta.";
                } else {
                    // --> 30 ENTREGADOS A TIEMPO (Camino feliz)
                    estado = EstadoEnvio.ENTREGADO;
                    fechaLlegadaReal = fechaEta.minusMinutes(30 + (i * 2)); // Llegó media hora o más antes del ETA
                    kgDestino = kgOrigen - 30; // Merma mínima
                    comentarios = "Descarga exitosa. Viaje finalizado en tiempo y forma.";
                }

                // Creamos el envío
                Envio env = Envio.builder()
                        .idEnvio("LT-HIST-" + String.format("%04d", i)) // ID forzado para identificar los históricos
                        .cpe("CPE-H-" + 8000 + i)
                        .autorizacionARCA("AUTH-H-" + 9000 + i)
                        .origen(origen)
                        .destino(destino)
                        .chofer(choferAsignado)
                        .camion(camionAsignado)
                        .tipoGrano(grano)
                        .estadoActual(estado)
                        .prioridadIa("BAJA")
                        .kgOrigen(kgOrigen)
                        .kgDestino(kgDestino)
                        .distanciaKm(150.0 + (i * 12))
                        .fechaCreacion(fechaCreacion)
                        .fechaSalida(fechaSalida)
                        .fechaEstimadaLlegada(fechaEta)
                        .fechaLlegada(fechaLlegadaReal)
                        .comentarios(comentarios)
                        .build();

                env = envioRepository.saveAndFlush(env);

                // 5. Agregamos el historial simulando el cierre del viaje
                historialEstadosRepository.saveAndFlush(HistorialEstados.builder()
                        .envio(env)
                        .usuario(op1) // Usamos el operador "op1" creado en el Paso 1
                        .tipoEvento(TipoEvento.CAMBIO_ESTADO)
                        .estadoAnterior((estado == EstadoEnvio.CANCELADO) ? EstadoEnvio.PENDIENTE : EstadoEnvio.EN_REPARTO)
                        .estadoNuevo(estado)
                        .fechaHora(fechaLlegadaReal != null ? fechaLlegadaReal : fechaCreacion.plusDays(1))
                        .build());
            }



           /*  LocalDate futuro = LocalDate.now().plusYears(1);
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
                    .passwordHash(passwordEncoder.encode("123456")).rol(RolUsuario.CHOFER).activo(false).build());
                        
          Usuario adm1 = usuarioRepository.saveAndFlush(Usuario.builder().username("administrador1")
                    .passwordHash(passwordEncoder.encode("123456")).rol(RolUsuario.ADMINISTRADOR).activo(true).build());

          
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
            Persona p7 = personaRepository.saveAndFlush(
                    Persona.builder().cuil("20-77777777-7").nombre("Sofia").apellido("Diaz").idUsuario(adm1).build());


            // 3. Choferes (Casos de prueba técnicos)
            ChoferDetalle cd1 = choferDetalleRepository.saveAndFlush(ChoferDetalle.builder().nroLicencia("LIC-100")
                    .vtoLicencia(futuro).vtoLinti(futuro).personaAsociada(p3).disponible(true).build());
            ChoferDetalle cd2 = choferDetalleRepository.saveAndFlush(ChoferDetalle.builder().nroLicencia("LIC-200")
                     .vtoLicencia(futuro).vtoLinti(futuro).personaAsociada(p4).disponible(true).build());
            // Chofer 3: Vencido pero VÁLIDO (Debería renovarse solo al crear un envío)
             ChoferDetalle cd3 = choferDetalleRepository.saveAndFlush(ChoferDetalle.builder()
                     .nroLicencia("LIC-OK-300").vtoLicencia(pasado).vtoLinti(pasado).personaAsociada(p5).disponible(true).build());
            // Chofer 4: Vencido e INVÁLIDO (Tiene 999, el Mock lo va a rebotar)
             ChoferDetalle cd4 = choferDetalleRepository.saveAndFlush(ChoferDetalle.builder()
                     .nroLicencia("LIC-999-BAD").vtoLicencia(pasado).vtoLinti(pasado).personaAsociada(p6).disponible(true).build());



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
                     .capacidadCargaKg(8500).vtoSenasa(futuro).disponible(true).build());
            Camion cam2 = camionRepository.saveAndFlush(Camion.builder().patente("AD456YY").rutaNro("RUTA-2")
                    .capacidadCargaKg(8200).vtoSenasa(futuro).disponible(true).build());
            // Camion 3: Vencido pero renovable
            Camion cam3 = camionRepository.saveAndFlush(Camion.builder().patente("AF789ZZ").rutaNro("RUTA-3")
                    .capacidadCargaKg(9000).vtoSenasa(pasado).disponible(true).build());
            // Camion 4: INHABILITADO (999 en patente)
                Camion cam4 = camionRepository.saveAndFlush(Camion.builder().patente("BAD-999").rutaNro("RUTA-4")
                        .capacidadCargaKg(8800).vtoSenasa(pasado).disponible(true).build());

            // 7. Envíos (10 Envíos con variedad de datos)
            LocalDateTime hoyExacto = LocalDateTime.now();

            for (int i = 1; i <= 10; i++) {
                // Truco: i % 4 dará resultados entre 0 y 3. 
                // Esto crea envíos de hoy, hace 1 día, hace 2 días y hace 3 días.
                LocalDateTime fechaDinamica = hoyExacto.minusDays(i % 4);

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
                        .fechaCreacion(fechaDinamica)
                        .build();

                env = envioRepository.saveAndFlush(env);

                // También le ponemos la fecha dinámica al historial para que coincida
                historialEstadosRepository.saveAndFlush(HistorialEstados.builder()
                .envio(env).
                usuario(op)
                .estadoNuevo(EstadoEnvio.PENDIENTE)
               .fechaHora(fechaDinamica)
                .build());
            }

            System.out.println("DATOS SEMILLA CARGADOS EXITOSAMENTE");
 */

                // VERSION 1
                /* *
                // =========================================================================
                // 8. HISTORIAL DE EVALUACIONES (Tarea #614 - Para la DEMO)
                // =========================================================================
                System.out.println("Cargando evaluaciones históricas para la demo...");

                // Buscamos un chofer existente (usamos cd1 creado en el punto 3)
                ChoferDetalle choferDemo = choferDetalleRepository.findAll().get(0);
                // Buscamos el primer envío creado en el bucle (el que terminó en la variable env)
                Envio envioDemo = envioRepository.findAll().get(0);

                for (int i = 1; i <= 12; i++) {
                EvaluacionPsicomotora eval = EvaluacionPsicomotora.builder()
                        .tiempoReaccionMs(350L) // Tiempo normal
                        .resultado(EstadoEvaluacionEnum.APROBADO)
                        .estadoBloqueo(EstadoEvaluacionEnum.APROBADO)
                        .mensaje("Test superado correctamente (Historial inicial).")
                        .fechaCreacion(LocalDateTime.now().minusDays(i))
                        // Vinculamos con las entidades reales:
                        .choferId(choferDemo) 
                        .idEnvio(envioDemo)
                        .build();
                
                evaluacionRepository.save(eval);
                }
                System.out.println("EVALUACIONES HISTÓRICAS CARGADAS");
                */

                //VERSION 2
                // =========================================================================
                // 8. HISTORIAL DE EVALUACIONES (Tarea #614 - Para la DEMO)
                // =========================================================================
                System.out.println("Cargando evaluaciones históricas para la demo...");
                List<ChoferDetalle> todosLosChoferes = choferDetalleRepository.findAll();
                List<Envio> todosLosEnvios = envioRepository.findAll();

                Random random = new Random(); // 2. Instancia el generador


                for (int i = 0; i < 12; i++) {
                        // Usamos el operador módulo (%) para rotar entre los choferes y envíos disponibles
                        ChoferDetalle choferActual = todosLosChoferes.get(i % todosLosChoferes.size());
                        Envio envioActual = todosLosEnvios.get(i % todosLosEnvios.size());
                        long tiempoAleatorio = 100 + (long)(random.nextDouble() * (umbralFatiga - 100));

                        EvaluacionPsicomotora eval = EvaluacionPsicomotora.builder()
                                .tiempoReaccionMs(tiempoAleatorio)
                                .resultado(EstadoEvaluacionEnum.APROBADO)
                                .estadoBloqueo(EstadoEvaluacionEnum.APROBADO)
                                .mensaje("Test superado correctamente (Historial inicial).")
                                .fechaCreacion(LocalDateTime.now().minusDays(i))
                                .choferId(choferActual) 
                                .idEnvio(envioActual)
                                .build();
                        
                        evaluacionRepository.save(eval);
                }
                System.out.println("EVALUACIONES HISTÓRICAS CARGADAS");

        } catch (Exception e) {
            throw new RuntimeException("ERROR EN DATA SEED: " + e.getMessage(), e);
        }
}
        private Establecimiento crearEstablecimiento(String nombre, String direccion, Double lat, Double lon, EmpresaCliente empresa) {
                return establecimientoRepository.saveAndFlush(Establecimiento.builder()
                        .nombreLugar(nombre)
                        .direccion(direccion)
                        .latitud(lat)
                        .longitud(lon)
                        .empresa(empresa)
                        .build());
        }

}
