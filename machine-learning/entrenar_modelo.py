import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score, classification_report
import joblib

# 1. Cargar el dataset generado (versión simple para granos)
df = pd.read_csv('dataset_granos_simple.csv')

# 2. Preprocesamiento: convertir variables categóricas a numéricas (One-Hot Encoding)
# Variables a transformar: destino, tipo_grano, cliente_tipo
df = pd.get_dummies(df, columns=['destino', 'tipo_grano', 'cliente_tipo'], drop_first=True)

# 3. Separar variables predictoras (X) y target (y)
X = df.drop('prioridad_envio', axis=1)   # todas las columnas menos la prioridad
y = df['prioridad_envio']                # target (Alta, Media, Baja)

# 4. Dividir en entrenamiento (80%) y prueba (20%)
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# 5. Entrenar Random Forest
modelo_rf = RandomForestClassifier(n_estimators=100, random_state=42)
modelo_rf.fit(X_train, y_train)

# 6. Evaluar el modelo
predicciones = modelo_rf.predict(X_test)
precision = accuracy_score(y_test, predicciones)

print("--- Métricas del Modelo de Prioridad (Granos) ---")
print(f"Precisión General: {precision * 100:.2f}%")
print("\nReporte detallado por clase:")
print(classification_report(y_test, predicciones))

# 7. Guardar modelo y nombres de columnas para futuras predicciones
joblib.dump(modelo_rf, 'modelo_prioridad_granos.pkl')
joblib.dump(X.columns, 'columnas_entrenamiento_granos.pkl')

print("Modelo exportado como 'modelo_prioridad_granos.pkl'")
print("Columnas de entrenamiento guardadas en 'columnas_entrenamiento_granos.pkl'")