package com.logitrack.sistema_logistica.model;

import java.time.LocalDateTime;
import jakarta.persistence.*; // Cambiado a jakarta
import com.logitrack.sistema_logistica.model.enums.EstadoEvaluacionEnum;
import com.logitrack.sistema_logistica.model.enums.TipoJuegoEnum;

import lombok.*;


@Entity
@Getter 
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder    
public class EvaluacionPsicomotora {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "chofer_id")
    private ChoferDetalle choferId;

    @ManyToOne @JoinColumn(name = "envio_id")
    private Envio idEnvio;

    @Enumerated(EnumType.STRING)
    private TipoJuegoEnum tipoJuego;

    private Long tiempoReaccionMs;

    @Enumerated(EnumType.STRING)
    private EstadoEvaluacionEnum estadoBloqueo; 

    @Enumerated(EnumType.STRING)
    private EstadoEvaluacionEnum resultado;

    private String mensaje;
    private String autorizadoPor; // Username del supervisor
    private String motivoAutorizacion;
    private LocalDateTime fechaCreacion = LocalDateTime.now();

}
