import pandas as pd
import numpy as np
import joblib
import json
import sys
sys.stdout.reconfigure(encoding='utf-8')
from pathlib import Path
from sklearn.ensemble import GradientBoostingRegressor
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler
from sklearn.compose import ColumnTransformer
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score

DATA_PATH = Path("viajes_historicos.csv")
MODEL_DIR = Path(".")
MODEL_DIR.mkdir(exist_ok=True)

# ── Cargar dataset ──────────────────────────────────────────────────────────
df = pd.read_csv(DATA_PATH)
print(f"Dataset cargado: {len(df)} registros")
print(f"Columnas: {list(df.columns)}")
print(f"\nEstadísticas de ETA (horas):\n{df['eta_horas'].describe()}\n")

# ── Features y target ──────────────────────────────────────────────────────
NUMERIC_FEATURES = [
    "distancia_km",
    "cantidad_paradas",
    "es_hora_pico",
    "es_fin_de_semana",
    "carga_kg",
    "tiene_refrigeracion",
]

CATEGORICAL_FEATURES = [
    "tipo_ruta",
    "condicion_climatica",
    "nivel_trafico",
    "tipo_vehiculo",
]

TARGET = "eta_horas"

X = df[NUMERIC_FEATURES + CATEGORICAL_FEATURES]
y = df[TARGET]

X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42
)
print(f"Train: {len(X_train)} | Test: {len(X_test)}")

# ── Pipeline de preprocesamiento + modelo ──────────────────────────────────
preprocessor = ColumnTransformer(
    transformers=[
        ("num", StandardScaler(), NUMERIC_FEATURES),
        ("cat", OneHotEncoder(handle_unknown="ignore", sparse_output=False), CATEGORICAL_FEATURES),
    ]
)

model = Pipeline(steps=[
    ("preprocessor", preprocessor),
    ("regressor", GradientBoostingRegressor(
        n_estimators=200,
        max_depth=5,
        learning_rate=0.08,
        subsample=0.85,
        min_samples_split=5,
        random_state=42,
    )),
])

# ── Entrenamiento ──────────────────────────────────────────────────────────
print("Entrenando modelo...")
model.fit(X_train, y_train)

# ── Evaluación ─────────────────────────────────────────────────────────────
y_pred = model.predict(X_test)

mae = mean_absolute_error(y_test, y_pred)
rmse = np.sqrt(mean_squared_error(y_test, y_pred))
r2 = r2_score(y_test, y_pred)

print(f"\n── Métricas en Test Set ──")
print(f"  MAE  (Error Absoluto Medio): {mae:.4f} horas ({mae*60:.1f} min)")
print(f"  RMSE (Raíz Error Cuadrático): {rmse:.4f} horas ({rmse*60:.1f} min)")
print(f"  R²   (Coeficiente determinación): {r2:.4f}")

cv_scores = cross_val_score(model, X, y, cv=5, scoring="neg_mean_absolute_error")
print(f"\n  CV MAE promedio: {-cv_scores.mean():.4f} horas ({-cv_scores.mean()*60:.1f} min)")

# ── Persistir modelo y metadata ────────────────────────────────────────────
model_path = MODEL_DIR / "eta_model.joblib"
joblib.dump(model, model_path)
print(f"\nModelo guardado en: {model_path}")

metadata = {
    "numeric_features": NUMERIC_FEATURES,
    "categorical_features": CATEGORICAL_FEATURES,
    "target": TARGET,
    "metrics": {
        "mae_horas": round(mae, 4),
        "mae_minutos": round(mae * 60, 1),
        "rmse_horas": round(rmse, 4),
        "r2_score": round(r2, 4),
    },
    "training_samples": len(X_train),
    "test_samples": len(X_test),
    "model_type": "GradientBoostingRegressor",
    "version": "1.0.0",
}

meta_path = MODEL_DIR / "model_metadata.json"
with open(meta_path, "w") as f:
    json.dump(metadata, f, indent=2, ensure_ascii=False)
print(f"Metadata guardada en: {meta_path}")

# ── Ejemplo de predicción ──────────────────────────────────────────────────
sample = pd.DataFrame([{
    "distancia_km": 350.0,
    "cantidad_paradas": 2,
    "es_hora_pico": 1,
    "es_fin_de_semana": 0,
    "carga_kg": 8000,
    "tiene_refrigeracion": 0,
    "tipo_ruta": "autopista",
    "condicion_climatica": "lluvia_leve",
    "nivel_trafico": "alto",
    "tipo_vehiculo": "camion_mediano",
}])

pred = model.predict(sample)[0]
print(f"\n── Predicción de ejemplo ──")
print(f"  Distancia: 350 km | Autopista | Lluvia leve | Tráfico alto | Hora pico")
print(f"  ETA predicho: {pred:.2f} horas ({pred*60:.0f} minutos)")
print("\n✓ Entrenamiento completado exitosamente.")
