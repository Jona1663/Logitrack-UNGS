import pandas as pd
import numpy as np

num_envios = 500
#np.random.seed(42)

# Generar datos
data = {
    "distancia_km": np.random.randint(50, 2000, num_envios),
    "destino": np.random.choice(["puerto", "otro"], num_envios, p=[0.4, 0.6]),
    "tipo_grano": np.random.choice(["soja", "trigo", "maiz", "girasol"], num_envios, p=[0.4, 0.3, 0.2, 0.1]),
    "cliente_tipo": np.random.choice(["estrategico", "normal"], num_envios, p=[0.3, 0.7])
}
df = pd.DataFrame(data)

# Función para calcular prioridad
def calcular_prioridad(row):
    puntaje = 0
    
    # Distancia
    if row["distancia_km"] > 1500:
        puntaje += 3
    elif row["distancia_km"] > 800:
        puntaje += 2
    elif row["distancia_km"] > 300:
        puntaje += 1
    
    # Destino puerto
    if row["destino"] == "puerto":
        puntaje += 1
    
    # Tipo de grano (soja o trigo)
    if row["tipo_grano"] in ["soja", "trigo"]:
        puntaje += 1
    
    # Cliente estratégico
    if row["cliente_tipo"] == "estrategico":
        puntaje += 2
    
    # Escala
    if puntaje >= 5:
        return "Alta"
    elif puntaje >= 3:
        return "Media"
    else:
        return "Baja"

df["prioridad_envio"] = df.apply(calcular_prioridad, axis=1)

# Guardar CSV
df.to_csv("dataset_granos_simple.csv", index=False)
print("Dataset generado: dataset_granos_simple.csv")
print("\n Distribución de prioridades:")
print(df["prioridad_envio"].value_counts())
print("\n Primeros 5 registros:")
print(df.head())