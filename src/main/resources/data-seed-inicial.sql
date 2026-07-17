-- 1. EMPRESAS CLIENTES
INSERT INTO empresas_clientes (cuit, razon_social, tipo_empresa, ruca_nro, ruca_estado) 
VALUES 
('30-11111111-2', 'Cargill Argentina', 'PUERTO', 'RUCA-001', true),
('30-22222222-3', 'Acopio Los Grobo', 'ACOPIO', 'RUCA-002', true);

-- 2. ESTABLECIMIENTOS
INSERT INTO establecimientos (cuit_empresa, nombre_lugar, direccion) 
VALUES 
('30-11111111-2', 'Puerto Rosario Planta 1', 'Av. Circunvalación S/N, Rosario'),
('30-22222222-3', 'Planta Acopio Pergamino', 'Ruta 8 Km 220, Pergamino');

-- 3. CAMIONES — agregado disponible = true
INSERT INTO camiones (patente, ruta_nro, vto_senasa, capacidad_Carga_kg, disponible) 
VALUES 
('AE123XX', 'Ruta Nac. 9', '2026-12-31', 8500, true),
('AD456YY', 'Ruta Nac. 8', '2026-10-15', 8200, true);

-- 4. USUARIOS
INSERT INTO usuarios (username, password_hash, rol, activo) 
VALUES 
('supervisor1', '123456', 'SUPERVISOR', true),
('operador1', '123456', 'OPERADOR', true),
('chofer1', '123456', 'CHOFER', true),
('chofer2', '123456', 'CHOFER', true);

-- 5. PERSONAS
INSERT INTO personas (id_usuario, cuil, nombre, apellido, telefono) 
VALUES 
(1, '20-11111111-1', 'Laura', 'Gomez', '1122334455'),
(2, '20-22222222-2', 'Martin', 'Rodriguez', '1133445566'),
(3, '20-33333333-3', 'Juan', 'Perez', '1144556677'),
(4, '20-44444444-4', 'Carlos', 'Lopez', '1155667788');

-- 6. CHOFERES DETALLE — agregado disponible = true
INSERT INTO choferes_detalle (id_chofer, nro_licencia, vto_licencia, vto_linti, disponible) 
VALUES 
(3, 'LIC-001', '2028-05-10', '2027-05-10', true),
(4, 'LIC-002', '2029-08-20', '2027-08-20', true);

-- 7. ENVIOS
INSERT INTO envios (id_envio, tracking_ctg, cpe, id_origen, id_destino, id_chofer, patente_camion, tipo_grano, estado_actual, prioridad_ia, kg_origen, kg_destino, fecha_creacion)
VALUES
('LT-A1B2C3', 'CTG-001', 'CPE-001', 2, 1, 3, 'AE123XX', 'SOJA', 'PENDIENTE', 'ALTA', 30000, NULL, CURRENT_TIMESTAMP),
('LT-X9Y8Z7', 'CTG-002', 'CPE-002', 2, 1, 4, 'AD456YY', 'MAIZ', 'PENDIENTE', 'MEDIA', 28000, NULL, CURRENT_TIMESTAMP),
('LT-M4N5P6', 'CTG-003', 'CPE-003', 2, 1, 3, 'AE123XX', 'TRIGO', 'PENDIENTE', 'BAJA', 32000, NULL, CURRENT_TIMESTAMP),
('LT-Q1W2E3', 'CTG-004', 'CPE-004', 2, 1, 4, 'AD456YY', 'GIRASOL', 'PENDIENTE', 'ALTA', 29000, NULL, CURRENT_TIMESTAMP),
('LT-R4T5Y6', 'CTG-005', 'CPE-005', 2, 1, 3, 'AE123XX', 'SORGO', 'PENDIENTE', 'MEDIA', 31000, NULL, CURRENT_TIMESTAMP);

-- 8. HISTORIAL
INSERT INTO historial_estados (id_envio, id_usuario, estado_anterior, estado_nuevo, fecha_hora)
VALUES
('LT-A1B2C3', 2, NULL, 'PENDIENTE', CURRENT_TIMESTAMP),
('LT-X9Y8Z7', 2, NULL, 'PENDIENTE', CURRENT_TIMESTAMP),
('LT-M4N5P6', 2, NULL, 'PENDIENTE', CURRENT_TIMESTAMP),
('LT-Q1W2E3', 2, NULL, 'PENDIENTE', CURRENT_TIMESTAMP),
('LT-R4T5Y6', 2, NULL, 'PENDIENTE', CURRENT_TIMESTAMP);