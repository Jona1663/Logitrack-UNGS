import csv
import random
from datetime import datetime, timedelta

random.seed(42)

ROUTE_TYPES = ["urbana", "rural", "autopista", "mixta"]
WEATHER_CONDITIONS = ["despejado", "lluvia_leve", "lluvia_fuerte", "niebla", "granizo"]
TRAFFIC_LEVELS = ["bajo", "moderado", "alto", "muy_alto"]
VEHICLE_TYPES = ["camion_pequeno", "camion_mediano", "camion_grande", "semi_trailer"]
CITY_PAIRS = [
    ("Buenos Aires", "La Plata"), ("Buenos Aires", "Rosario"), ("Buenos Aires", "Cordoba"),
    ("Buenos Aires", "Mar del Plata"), ("Rosario", "Cordoba"), ("Cordoba", "Mendoza"),
    ("Buenos Aires", "Mendoza"), ("Santa Fe", "Buenos Aires"), ("Tucuman", "Cordoba"),
    ("San Miguel", "CABA"), ("Moron", "Palermo"), ("Quilmes", "San Isidro"),
    ("Lomas de Zamora", "Pilar"), ("Tigre", "Ezeiza"), ("Lanus", "San Martin"),
]

BASE_SPEED = {"urbana": 35, "rural": 70, "autopista": 100, "mixta": 55}
WEATHER_FACTOR = {
    "despejado": 1.0, "lluvia_leve": 0.88, "lluvia_fuerte": 0.72,
    "niebla": 0.78, "granizo": 0.65
}
TRAFFIC_FACTOR = {"bajo": 1.0, "moderado": 0.80, "alto": 0.60, "muy_alto": 0.40}
VEHICLE_FACTOR = {"camion_pequeno": 1.05, "camion_mediano": 1.0, "camion_grande": 0.90, "semi_trailer": 0.80}

def calc_eta(distance_km, route_type, weather, traffic, vehicle_type, stops=0):
    speed = BASE_SPEED[route_type]
    speed *= WEATHER_FACTOR[weather]
    speed *= TRAFFIC_FACTOR[traffic]
    speed *= VEHICLE_FACTOR[vehicle_type]
    base_hours = distance_km / speed
    stop_penalty = stops * random.uniform(0.25, 0.45)
    noise = random.gauss(0, base_hours * 0.06)
    total_hours = max(0.25, base_hours + stop_penalty + noise)
    return round(total_hours, 4)

records = []
start_date = datetime(2022, 1, 1)

for i in range(1000):
    city_pair = random.choice(CITY_PAIRS)
    route_type = random.choice(ROUTE_TYPES)
    weather = random.choices(WEATHER_CONDITIONS, weights=[50, 20, 10, 12, 8])[0]
    traffic = random.choices(TRAFFIC_LEVELS, weights=[25, 40, 25, 10])[0]
    vehicle = random.choice(VEHICLE_TYPES)

    distance_km = round(random.uniform(10, 1400), 1)
    stops = random.randint(0, 5)

    hour = random.randint(0, 23)
    is_rush_hour = 1 if hour in [7, 8, 9, 17, 18, 19] else 0
    is_weekend = 1 if random.random() < 0.28 else 0
    load_kg = random.randint(500, 25000)
    has_refrigeration = 1 if random.random() < 0.2 else 0

    trip_date = start_date + timedelta(days=random.randint(0, 800))

    if is_rush_hour:
        traffic_adj = {"bajo": "moderado", "moderado": "alto", "alto": "muy_alto", "muy_alto": "muy_alto"}.get(traffic, traffic)
    else:
        traffic_adj = traffic

    eta_hours = calc_eta(distance_km, route_type, weather, traffic_adj, vehicle, stops)
    eta_minutes = round(eta_hours * 60)

    records.append({
        "viaje_id": f"VJ-{10000 + i}",
        "fecha": trip_date.strftime("%Y-%m-%d"),
        "hora_salida": f"{hour:02d}:00",
        "origen": city_pair[0],
        "destino": city_pair[1],
        "distancia_km": distance_km,
        "tipo_ruta": route_type,
        "condicion_climatica": weather,
        "nivel_trafico": traffic_adj,
        "tipo_vehiculo": vehicle,
        "cantidad_paradas": stops,
        "es_hora_pico": is_rush_hour,
        "es_fin_de_semana": is_weekend,
        "carga_kg": load_kg,
        "tiene_refrigeracion": has_refrigeration,
        "eta_horas": eta_hours,
        "eta_minutos": eta_minutes,
    })

output_path = "viajes_historicos.csv"
with open(output_path, "w", newline="", encoding="utf-8") as f:
    writer = csv.DictWriter(f, fieldnames=records[0].keys())
    writer.writeheader()
    writer.writerows(records)

print(f"Dataset generado: {len(records)} registros en {output_path}")
print(f"\nPrimeros 3 registros:")
for r in records[:3]:
    print(r)
