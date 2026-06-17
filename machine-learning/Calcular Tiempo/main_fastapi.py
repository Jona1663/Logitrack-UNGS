"""
Microservicio de predicción de ETA - Tarea #387
FastAPI + scikit-learn
Endpoint: POST /api/v1/eta/predict
"""
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import Optional, Literal
import joblib
import pandas as pd
import json
import time
import logging
from pathlib import Path

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

MODEL_DIR = Path(__file__).parent.parent / "model"
MODEL_PATH = MODEL_DIR / "eta_model.joblib"
META_PATH = MODEL_DIR / "model_metadata.json"

app = FastAPI(
    title="ETA Prediction Microservice",
    description="Predice el Tiempo Estimado de Llegada basado en características del viaje y datos históricos",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Carga del modelo
model = None
model_metadata = {}

@app.on_event("startup")
async def load_model():
    global model, model_metadata
    try:
        model = joblib.load(MODEL_PATH)
        with open(META_PATH) as f:
            model_metadata = json.load(f)
        logger.info(f"Modelo cargado exitosamente. R²={model_metadata['metrics']['r2_score']}")
    except Exception as e:
        logger.error(f"Error al cargar el modelo: {e}")
        raise RuntimeError(f"No se pudo cargar el modelo: {e}")


# Schemas 
class ETARequest(BaseModel):
    distancia_km: float = Field(..., gt=0, le=5000, description="Distancia del trayecto en kilómetros")
    tipo_ruta: Literal["urbana", "rural", "autopista", "mixta"] = Field(..., description="Tipo de ruta")
    condicion_climatica: Literal["despejado", "lluvia_leve", "lluvia_fuerte", "niebla", "granizo"] = Field(
        default="despejado", description="Condición climática"
    )
    nivel_trafico: Literal["bajo", "moderado", "alto", "muy_alto"] = Field(
        default="moderado", description="Nivel de tráfico esperado"
    )
    tipo_vehiculo: Literal["camion_pequeno", "camion_mediano", "camion_grande", "semi_trailer"] = Field(
        default="camion_mediano", description="Tipo de vehículo"
    )
    cantidad_paradas: int = Field(default=0, ge=0, le=20, description="Número de paradas intermedias")
    es_hora_pico: int = Field(default=0, ge=0, le=1, description="1 si es hora pico (7-9h, 17-19h)")
    es_fin_de_semana: int = Field(default=0, ge=0, le=1, description="1 si es fin de semana")
    carga_kg: float = Field(default=5000, gt=0, le=30000, description="Peso de la carga en kg")
    tiene_refrigeracion: int = Field(default=0, ge=0, le=1, description="1 si requiere refrigeración")

    class Config:
        json_schema_extra = {
            "example": {
                "distancia_km": 350.0,
                "tipo_ruta": "autopista",
                "condicion_climatica": "despejado",
                "nivel_trafico": "moderado",
                "tipo_vehiculo": "camion_mediano",
                "cantidad_paradas": 1,
                "es_hora_pico": 0,
                "es_fin_de_semana": 0,
                "carga_kg": 8000,
                "tiene_refrigeracion": 0,
            }
        }


class ETAResponse(BaseModel):
    eta_horas: float = Field(..., description="Tiempo estimado en horas")
    eta_minutos: int = Field(..., description="Tiempo estimado en minutos")
    eta_formateado: str = Field(..., description="Tiempo en formato legible (ej: '4h 35min')")
    confianza: str = Field(..., description="Nivel de confianza de la predicción")
    modelo_version: str
    tiempo_procesamiento_ms: float


class HealthResponse(BaseModel):
    status: str
    modelo_cargado: bool
    version: str
    metricas: dict


# Helpers 
def format_eta(hours: float) -> str:
    total_minutes = int(round(hours * 60))
    h = total_minutes // 60
    m = total_minutes % 60
    if h == 0:
        return f"{m}min"
    return f"{h}h {m:02d}min"


def get_confidence(distancia_km: float, condicion_climatica: str) -> str:
    """
    Heurística simple de confianza:
    - Distancias cortas + clima normal → alta
    - Condiciones adversas o rutas muy largas → media
    - Granizo/lluvia fuerte + larga distancia → baja
    """
    adverse = condicion_climatica in ["lluvia_fuerte", "granizo", "niebla"]
    if distancia_km < 100 and not adverse:
        return "alta"
    elif distancia_km > 800 or adverse:
        return "media" if not (adverse and distancia_km > 500) else "baja"
    return "alta"


# ── Endpoints
@app.get("/health", response_model=HealthResponse, tags=["Sistema"])
async def health_check():
    """Verifica el estado del microservicio y del modelo cargado."""
    return HealthResponse(
        status="ok" if model is not None else "degraded",
        modelo_cargado=model is not None,
        version=model_metadata.get("version", "unknown"),
        metricas=model_metadata.get("metrics", {}),
    )


@app.post("/api/v1/eta/predict", response_model=ETAResponse, tags=["Predicción"])
async def predict_eta(request: ETARequest):
    """
    Predice el ETA para un viaje dado sus características.
    Usado por EnvioService al momento de asignar camión y chofer.
    """
    if model is None:
        raise HTTPException(
            status_code=503,
            detail="Modelo no disponible. El servicio está iniciando o presenta fallas.",
        )

    t_start = time.perf_counter()

    try:
        features = pd.DataFrame([{
            "distancia_km": request.distancia_km,
            "cantidad_paradas": request.cantidad_paradas,
            "es_hora_pico": request.es_hora_pico,
            "es_fin_de_semana": request.es_fin_de_semana,
            "carga_kg": request.carga_kg,
            "tiene_refrigeracion": request.tiene_refrigeracion,
            "tipo_ruta": request.tipo_ruta,
            "condicion_climatica": request.condicion_climatica,
            "nivel_trafico": request.nivel_trafico,
            "tipo_vehiculo": request.tipo_vehiculo,
        }])

        eta_horas = float(model.predict(features)[0])
        eta_horas = max(0.1, eta_horas)

        t_end = time.perf_counter()
        proc_ms = round((t_end - t_start) * 1000, 2)

        logger.info(
            f"ETA predicho: {eta_horas:.2f}h para {request.distancia_km}km "
            f"via {request.tipo_ruta} | {proc_ms}ms"
        )

        return ETAResponse(
            eta_horas=round(eta_horas, 2),
            eta_minutos=int(round(eta_horas * 60)),
            eta_formateado=format_eta(eta_horas),
            confianza=get_confidence(request.distancia_km, request.condicion_climatica),
            modelo_version=model_metadata.get("version", "1.0.0"),
            tiempo_procesamiento_ms=proc_ms,
        )

    except Exception as e:
        logger.error(f"Error en predicción: {e}")
        raise HTTPException(status_code=500, detail=f"Error interno al calcular ETA: {str(e)}")


@app.get("/", tags=["Sistema"])
async def root():
    return {
        "servicio": "ETA Prediction Microservice",
        "version": "1.0.0",
        "docs": "/docs",
        "health": "/health",
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=False)
