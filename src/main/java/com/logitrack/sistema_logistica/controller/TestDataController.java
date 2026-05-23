package com.logitrack.sistema_logistica.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logitrack.sistema_logistica.model.ChoferDetalle;
import com.logitrack.sistema_logistica.model.Camion;
import com.logitrack.sistema_logistica.model.Establecimiento;
import com.logitrack.sistema_logistica.model.Envio;
import com.logitrack.sistema_logistica.model.HistorialEstados;
import com.logitrack.sistema_logistica.model.Usuario;
import com.logitrack.sistema_logistica.model.enums.EstadoEnvio;
import com.logitrack.sistema_logistica.model.enums.TipoGrano;
import com.logitrack.sistema_logistica.repository.ChoferDetalleRepository;
import com.logitrack.sistema_logistica.repository.CamionRepository;
import com.logitrack.sistema_logistica.repository.EstablecimientoRepository;
import com.logitrack.sistema_logistica.repository.EnvioRepository;
import com.logitrack.sistema_logistica.repository.HistorialEstadosRepository;
import com.logitrack.sistema_logistica.repository.UsuarioRepository;

@RestController
@RequestMapping("/api/test")
public class TestDataController {

    @Autowired
    private EnvioRepository envioRepository;
    @Autowired
    private ChoferDetalleRepository choferDetalleRepository;
    @Autowired
    private CamionRepository camionRepository;
    @Autowired
    private EstablecimientoRepository establecimientoRepository;
    @Autowired
    private HistorialEstadosRepository historialEstadosRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/generar-datos")
    public ResponseEntity<?> generarDatos() {
        try {
            // Obtenemos datos base
            List<ChoferDetalle> choferes = choferDetalleRepository.findAll();
            List<Camion> camiones = camionRepository.findAll();
            List<Establecimiento> establecimientos = establecimientoRepository.findAll();
            Usuario operador = usuarioRepository.findAll().stream()
                    .filter(u -> u.getRol().name().equals("OPERADOR"))
                    .findFirst()
                    .orElse(usuarioRepository.findAll().get(0));

            if (choferes.isEmpty() || camiones.isEmpty() || establecimientos.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("❌ Faltan datos base:\n"
                                + "- Choferes: " + choferes.size() + "\n"
                                + "- Camiones: " + camiones.size() + "\n"
                                + "- Establecimientos: " + establecimientos.size());
            }

            int contador = 0;
            int errorCount = 0;
            StringBuilder resultados = new StringBuilder();

            // 15 PENDIENTE
            for (int i = 0; i < 15; i++) {
                try {
                    Envio env = Envio.builder()
                            .cpe("CPE-TEST-P" + (1000 + contador))
                            .autorizacionARCA("AUTH-TEST-" + (1000 + contador))
                            .origen(establecimientos.get(i % establecimientos.size()))
                            .destino(establecimientos.get((i + 1) % establecimientos.size()))
                            .chofer(choferes.get(i % choferes.size()))
                            .camion(camiones.get(i % camiones.size()))
                            .tipoGrano(i % 2 == 0 ? TipoGrano.SOJA : TipoGrano.MAIZ)
                            .estadoActual(EstadoEnvio.PENDIENTE)
                            .prioridadIa(i < 8 ? "ALTA" : "MEDIA")
                            .kgOrigen(5000 + (i * 500))
                            .distanciaKm(100.0 + (i * 10))
                            .build();

                    envioRepository.saveAndFlush(env);
                    historialEstadosRepository.saveAndFlush(HistorialEstados.builder().envio(env).usuario(operador)
                            .estadoNuevo(EstadoEnvio.PENDIENTE).build());
                    contador++;
                } catch (Exception e) {
                    errorCount++;
                    resultados.append("Error en PENDIENTE " + i + ": " + e.getMessage() + "\n");
                }
            }

            // 4 ENTREGADO
            for (int i = 0; i < 4; i++) {
                try {
                    LocalDateTime fechaLlegada = LocalDateTime.now().minusDays(3 + i).plusHours(12 + (i * 2));

                    Envio env = Envio.builder()
                            .cpe("CPE-TEST-E" + (1000 + contador))
                            .autorizacionARCA("AUTH-TEST-" + (1000 + contador))
                            .origen(establecimientos.get(i % establecimientos.size()))
                            .destino(establecimientos.get((i + 1) % establecimientos.size()))
                            .chofer(choferes.get(i % choferes.size()))
                            .camion(camiones.get(i % camiones.size()))
                            .tipoGrano(i % 2 == 0 ? TipoGrano.SOJA : TipoGrano.MAIZ)
                            .estadoActual(EstadoEnvio.ENTREGADO)
                            .prioridadIa("MEDIA")
                            .kgOrigen(6000 + (i * 500))
                            .kgDestino(6000 + (i * 500))
                            .distanciaKm(150.0 + (i * 10))
                            .fechaLlegada(fechaLlegada)
                            .build();

                    envioRepository.saveAndFlush(env);
                    historialEstadosRepository.saveAndFlush(HistorialEstados.builder().envio(env).usuario(operador)
                            .estadoNuevo(EstadoEnvio.ENTREGADO).build());
                    contador++;
                } catch (Exception e) {
                    errorCount++;
                    resultados.append("Error en ENTREGADO " + i + ": " + e.getMessage() + "\n");
                }
            }

            // 1 CANCELADO
            try {
                Envio envCancelado = Envio.builder()
                        .cpe("CPE-TEST-C" + 1000)
                        .autorizacionARCA("AUTH-TEST-" + 1000)
                        .origen(establecimientos.get(0))
                        .destino(establecimientos.get(1))
                        .chofer(choferes.get(0))
                        .camion(camiones.get(0))
                        .tipoGrano(TipoGrano.SOJA)
                        .estadoActual(EstadoEnvio.CANCELADO)
                        .prioridadIa("BAJA")
                        .kgOrigen(5000)
                        .distanciaKm(120.0)
                        .build();

                envioRepository.saveAndFlush(envCancelado);
                historialEstadosRepository.saveAndFlush(HistorialEstados.builder().envio(envCancelado).usuario(operador)
                        .estadoNuevo(EstadoEnvio.CANCELADO).build());
                contador++;
            } catch (Exception e) {
                errorCount++;
                resultados.append("Error en CANCELADO: " + e.getMessage() + "\n");
            }

            // 1 EN_REPARTO
            try {
                Envio envEnReparto = Envio.builder()
                        .cpe("CPE-TEST-R" + 1000)
                        .autorizacionARCA("AUTH-TEST-" + 1001)
                        .origen(establecimientos.get(2 % establecimientos.size()))
                        .destino(establecimientos.get((2 + 1) % establecimientos.size()))
                        .chofer(choferes.get(1 % choferes.size()))
                        .camion(camiones.get(1 % camiones.size()))
                        .tipoGrano(TipoGrano.MAIZ)
                        .estadoActual(EstadoEnvio.EN_REPARTO)
                        .prioridadIa("ALTA")
                        .kgOrigen(5500)
                        .distanciaKm(110.0)
                        .build();

                envioRepository.saveAndFlush(envEnReparto);
                historialEstadosRepository.saveAndFlush(HistorialEstados.builder().envio(envEnReparto).usuario(operador)
                        .estadoNuevo(EstadoEnvio.EN_REPARTO).build());
                contador++;
            } catch (Exception e) {
                errorCount++;
                resultados.append("Error en EN_REPARTO: " + e.getMessage() + "\n");
            }

            // 1 EN_TRANSITO
            try {
                Envio envEnTransito = Envio.builder()
                        .cpe("CPE-TEST-T" + 1000)
                        .autorizacionARCA("AUTH-TEST-" + 1002)
                        .origen(establecimientos.get(1 % establecimientos.size()))
                        .destino(establecimientos.get(2 % establecimientos.size()))
                        .chofer(choferes.get((2 % choferes.size())))
                        .camion(camiones.get(1 % camiones.size()))
                        .tipoGrano(TipoGrano.SOJA)
                        .estadoActual(EstadoEnvio.EN_TRANSITO)
                        .prioridadIa("MEDIA")
                        .kgOrigen(6500)
                        .distanciaKm(140.0)
                        .build();

                envioRepository.saveAndFlush(envEnTransito);
                historialEstadosRepository.saveAndFlush(HistorialEstados.builder().envio(envEnTransito).usuario(operador)
                        .estadoNuevo(EstadoEnvio.EN_TRANSITO).build());
            } catch (Exception e) {
                errorCount++;
                resultados.append("Error en EN_TRANSITO: " + e.getMessage() + "\n");
            }

            resultados.insert(0, "✅ Datos de prueba generados exitosamente!\n\n");
            resultados.append("\nTotal creado: ").append(contador).append(" envíos\n");
            if (errorCount > 0) {
                resultados.append("Errores: ").append(errorCount).append("\n");
            }

            return ResponseEntity.ok().body(resultados.toString());

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("❌ Error fatal: " + e.getMessage() + "\n" + e.getCause());
        }
    }
}
