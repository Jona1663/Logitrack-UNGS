"""
Integración del microservicio de ETA con el módulo de asignación - Tarea #387

IMPORTANTE: NO se agregan métodos a EnvioService.
La petición HTTP al microservicio de IA se invoca ÚNICAMENTE en el momento
de asignar camión y chofer (endpoint/método de asignación existente).

Patrón: ETAClient → llama al microservicio → resultado se inyecta en la asignación.
"""

import httpx
import logging
from dataclasses import dataclass
from typing import Optional

logger = logging.getLogger(__name__)

ETA_SERVICE_URL = "http://localhost:8000"   # configurable via env var
ETA_TIMEOUT_SECONDS = 5.0                   # Timeout para tolerancia a fallos (Escenario 3)


# ── Data classes ─────────────────────────────────────────────────────────────

@dataclass
class ETAResult:
    """Resultado de la predicción de ETA."""
    eta_horas: float
    eta_minutos: int
    eta_formateado: str          # "4h 35min"
    confianza: str               # "alta" | "media" | "baja"
    disponible: bool = True      # False si el servicio está caído
    mensaje: str = ""            # Descripción del estado


ETA_PENDIENTE = ETAResult(
    eta_horas=0.0,
    eta_minutos=0,
    eta_formateado="Pendiente de cálculo",
    confianza="",
    disponible=False,
    mensaje="El servicio de predicción no está disponible. Ingrese el ETA manualmente.",
)


# ── Cliente HTTP del microservicio ETA ────────────────────────────────────────

class ETAClient:
    """
    Cliente liviano para el microservicio de predicción de ETA.
    Diseñado para ser invocado al momento de asignar camión y chofer.

    Tolerancia a fallos (Escenario 3):
    - Si el servicio está caído → retorna ETA_PENDIENTE sin bloquear la operación.
    - Si la latencia supera ETA_TIMEOUT_SECONDS → ídem.
    - La operación principal (guardar asignación) NUNCA se bloquea por este servicio.
    """

    def __init__(self, base_url: str = ETA_SERVICE_URL, timeout: float = ETA_TIMEOUT_SECONDS):
        self.base_url = base_url
        self.timeout = timeout

    def calcular_eta(
        self,
        distancia_km: float,
        tipo_ruta: str,
        condicion_climatica: str = "despejado",
        nivel_trafico: str = "moderado",
        tipo_vehiculo: str = "camion_mediano",
        cantidad_paradas: int = 0,
        es_hora_pico: int = 0,
        es_fin_de_semana: int = 0,
        carga_kg: float = 5000,
        tiene_refrigeracion: int = 0,
    ) -> ETAResult:
        """
        Realiza la petición HTTP al microservicio de IA y retorna la predicción.
        En caso de fallo, retorna ETA_PENDIENTE para no bloquear la asignación.
        """
        payload = {
            "distancia_km": distancia_km,
            "tipo_ruta": tipo_ruta,
            "condicion_climatica": condicion_climatica,
            "nivel_trafico": nivel_trafico,
            "tipo_vehiculo": tipo_vehiculo,
            "cantidad_paradas": cantidad_paradas,
            "es_hora_pico": es_hora_pico,
            "es_fin_de_semana": es_fin_de_semana,
            "carga_kg": carga_kg,
            "tiene_refrigeracion": tiene_refrigeracion,
        }

        try:
            with httpx.Client(timeout=self.timeout) as client:
                response = client.post(
                    f"{self.base_url}/api/v1/eta/predict",
                    json=payload,
                )
                response.raise_for_status()
                data = response.json()

            logger.info(
                f"ETA calculado: {data['eta_formateado']} "
                f"para {distancia_km}km via {tipo_ruta}"
            )
            return ETAResult(
                eta_horas=data["eta_horas"],
                eta_minutos=data["eta_minutos"],
                eta_formateado=data["eta_formateado"],
                confianza=data["confianza"],
                disponible=True,
            )

        except httpx.TimeoutException:
            logger.warning(
                f"Timeout al llamar microservicio ETA ({self.timeout}s). "
                "La asignación continuará con ETA pendiente."
            )
            return ETA_PENDIENTE

        except httpx.HTTPStatusError as e:
            logger.warning(f"Error HTTP del microservicio ETA: {e.response.status_code}")
            return ETA_PENDIENTE

        except Exception as e:
            logger.warning(f"Microservicio ETA no disponible: {e}")
            return ETA_PENDIENTE


# ── Singleton del cliente (reutilizable en toda la aplicación) ────────────────
eta_client = ETAClient()


# ── Integración con el flujo de asignación de camión y chofer ─────────────────
#
# INSTRUCCIÓN DE USO:
# En el método/endpoint existente donde se guarda la asignación de camión+chofer,
# agregar la llamada a eta_client.calcular_eta() ANTES de persistir.
#
# El resultado se incluye en los datos de la asignación. Si el servicio falla,
# eta_formateado = "Pendiente de cálculo" y disponible = False,
# lo que permite al operador ingresarlo manualmente.
#
# Ejemplo de uso dentro del handler de asignación existente:
#
# ──────────────────────────────────────────────────────────────────────────────
# from eta_integration import eta_client
#
# def asignar_camion_y_chofer(envio_id, camion_id, chofer_id, ...):
#     # ... lógica existente de validación y asignación ...
#
#     eta = eta_client.calcular_eta(
#         distancia_km=envio.distancia_km,
#         tipo_ruta=envio.tipo_ruta,
#         condicion_climatica=get_clima_actual(),   # de tu servicio de clima
#         nivel_trafico=get_trafico_actual(),        # de tu fuente de tráfico
#         tipo_vehiculo=camion.tipo,
#         cantidad_paradas=envio.paradas_intermedias,
#         es_hora_pico=1 if hora_actual in HORAS_PICO else 0,
#         es_fin_de_semana=1 if es_fin_de_semana() else 0,
#         carga_kg=envio.peso_carga_kg,
#         tiene_refrigeracion=1 if envio.requiere_frio else 0,
#     )
#
#     # Guardar la asignación (siempre, independientemente del ETA)
#     asignacion = Asignacion(
#         envio_id=envio_id,
#         camion_id=camion_id,
#         chofer_id=chofer_id,
#         eta_horas=eta.eta_horas if eta.disponible else None,
#         eta_minutos=eta.eta_minutos if eta.disponible else None,
#         eta_formateado=eta.eta_formateado,      # "4h 35min" o "Pendiente de cálculo"
#         eta_disponible=eta.disponible,          # False → mostrar campo manual en UI
#         eta_confianza=eta.confianza,
#     )
#     db.save(asignacion)
#     return asignacion
# ──────────────────────────────────────────────────────────────────────────────


# ── Prueba de integración (ejecutar directamente para verificar) ──────────────
if __name__ == "__main__":
    import os

    print("── Test 1: Servicio disponible ──")
    eta = eta_client.calcular_eta(
        distancia_km=350.0,
        tipo_ruta="autopista",
        condicion_climatica="despejado",
        nivel_trafico="moderado",
        tipo_vehiculo="camion_mediano",
        cantidad_paradas=1,
        es_hora_pico=0,
        carga_kg=8000,
    )
    if eta.disponible:
        print(f"  ✓ ETA: {eta.eta_formateado} ({eta.eta_horas}h) | Confianza: {eta.confianza}")
    else:
        print(f"  ⚠ Servicio no disponible → {eta.mensaje}")

    print("\n── Test 2: Servicio caído (tolerancia a fallos, Escenario 3) ──")
    client_caido = ETAClient(base_url="http://localhost:9999", timeout=1.0)
    eta_fallback = client_caido.calcular_eta(distancia_km=200, tipo_ruta="urbana")
    print(f"  ETA formateado: '{eta_fallback.eta_formateado}'")
    print(f"  Disponible: {eta_fallback.disponible}")
    print(f"  Operación bloqueada: NO → la asignación puede guardarse igual")
    print(f"  Mensaje: {eta_fallback.mensaje}")
