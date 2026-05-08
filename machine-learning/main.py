from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
import pandas as pd
import joblib
from fastapi.middleware.cors import CORSMiddleware

app = FastAPI(
    title="API Predictiva - LogiTrack",
    description="Microservicio para calcular la prioridad de envíos de granos usando Random Forest.",
    version="2.0.0"
)

# 
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], 
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 3. Cargar modelo al iniciar
try:
    modelo = joblib.load("modelo_prioridad_granos.pkl")
    columnas_entrenamiento = joblib.load("columnas_entrenamiento_granos.pkl")
    print("Modelo cargado correctamente")
except Exception as e:
    print(f"Error cargando modelo: {e}")
    modelo = None
    columnas_entrenamiento = None


# Esquema de entrada
class DatosEnvio(BaseModel):
    distancia_km: int = Field(..., description="Distancia del envío en km")
    destino: str = Field(..., description="puerto u otro")
    tipo_grano: str = Field(..., description="soja, trigo, maiz, girasol")
    cliente_tipo: str = Field(..., description="estrategico o normal")


# 
@app.get("/", summary="Health check")
def health():
    return {"status": "ok", "mensaje": "API funcionando"}


# 6. Endpoint de predicción
@app.post("/predecir-prioridad", summary="Predice la prioridad de un envío")
def predecir_prioridad(envio: DatosEnvio):

    if modelo is None:
        raise HTTPException(status_code=500, detail="Modelo no cargado")

    try:
        
        df = pd.DataFrame([envio.dict()])  
        df = pd.get_dummies(df, columns=['destino', 'tipo_grano', 'cliente_tipo'])
        df = df.reindex(columns=columnas_entrenamiento, fill_value=0)

        prediccion = modelo.predict(df)[0]

        return {
            "estado": "exito",
            "prioridad": prediccion
        }

    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Error en predicción: {str(e)}"
        )