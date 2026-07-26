-- ============================================================================
-- SEED 01 — Identidad (Ecuador, 2 tiendas)  ·  se ejecuta por SQL directo
-- Tienda 1 = empresa del bootstrap, repurposed → "Fiesta & Fantasía" (Quito)
-- Tienda 2 = nueva → "El Baúl del Disfraz" (Cuenca)
-- Password de TODOS los sembrados: Passw0rd!   (bcrypt abajo)
-- Idempotente: usa ids fijos + ON CONFLICT; borra filas hijas antes de reinsertar.
-- ============================================================================
\set ON_ERROR_STOP on

-- bcrypt de 'Passw0rd!'
\set HASH '''$2a$10$5G7opvsbUoc4KwNHN/nU7elpx8BgVZ4.ey79M0lj/d7AxIkpAJ3Fa'''

-- ---------- limpieza de prueba ----------
DELETE FROM categoria WHERE nombre = '__probe_categoria';

-- ============================================================================
-- TIENDA 1  (empresa del bootstrap)  → Fiesta & Fantasía (Quito)
-- ============================================================================
UPDATE empresa
   SET nombre = 'Fiesta & Fantasía', ciudad = 'Quito'
 WHERE id = (SELECT empresa_id FROM usuario WHERE email = 'dueno@costumi.co');

-- dueño 1: reset de password para poder operar por API
UPDATE usuario SET password_hash = :HASH WHERE email = 'dueno@costumi.co';

-- config de la Tienda 1 (no existía) — multi-sucursal ON, IVA 15%, USD
INSERT INTO configuracion_empresa
  (empresa_id, conteo_stock, multas_activo, multi_sucursal, pago_en_linea,
   tasa_impuesto, moneda, recargo_retraso_dia, modo_recargo_retraso,
   reembolsos_activos, ventana_reembolso_dias)
VALUES
  ((SELECT empresa_id FROM usuario WHERE email='dueno@costumi.co'),
   true, true, true, false, 0.15, 'USD', 2, 'ACUMULATIVA', true, 8)
ON CONFLICT (empresa_id) DO UPDATE SET
   multi_sucursal = true, tasa_impuesto = 0.15, moneda = 'USD',
   recargo_retraso_dia = 2, reembolsos_activos = true, ventana_reembolso_dias = 8;

-- S1a: Casa Matriz → Centro Histórico
UPDATE sucursal
   SET nombre = 'Centro Histórico', direccion = 'García Moreno N4-20, Quito'
 WHERE empresa_id = (SELECT empresa_id FROM usuario WHERE email='dueno@costumi.co')
   AND nombre = 'Casa Matriz';

-- S1b: Cumbayá (id fijo)
INSERT INTO sucursal (id, empresa_id, nombre, direccion, archivada)
VALUES ('51b51b51-0000-0000-0000-000000000001',
        (SELECT empresa_id FROM usuario WHERE email='dueno@costumi.co'),
        'Cumbayá', 'Av. Interoceánica km 12, Cumbayá', false)
ON CONFLICT (id) DO UPDATE SET nombre = EXCLUDED.nombre, direccion = EXCLUDED.direccion;

-- ============================================================================
-- TIENDA 2  (nueva)  → El Baúl del Disfraz (Cuenca)
-- ============================================================================
INSERT INTO empresa (id, nombre, estado, fecha_registro, ciudad)
VALUES ('e2e2e2e2-0000-0000-0000-0000000000e2', 'El Baúl del Disfraz', 'ACTIVA', now(), 'Cuenca')
ON CONFLICT (id) DO UPDATE SET nombre = EXCLUDED.nombre, estado = 'ACTIVA', ciudad = 'Cuenca';

INSERT INTO configuracion_empresa
  (empresa_id, conteo_stock, multas_activo, multi_sucursal, pago_en_linea,
   tasa_impuesto, moneda, recargo_retraso_dia, modo_recargo_retraso,
   reembolsos_activos, ventana_reembolso_dias)
VALUES
  ('e2e2e2e2-0000-0000-0000-0000000000e2',
   true, true, false, false, 0.15, 'USD', 2, 'ACUMULATIVA', true, 8)
ON CONFLICT (empresa_id) DO UPDATE SET tasa_impuesto = 0.15, moneda = 'USD';

-- dueño 2
INSERT INTO usuario (id, empresa_id, email, password_hash, rol, activo)
VALUES ('d2d2d2d2-0000-0000-0000-0000000000d2', 'e2e2e2e2-0000-0000-0000-0000000000e2',
        'dueno@baul.ec', :HASH, 'DUENO', true)
ON CONFLICT (id) DO UPDATE SET password_hash = EXCLUDED.password_hash, empresa_id = EXCLUDED.empresa_id;

-- S2: Cuenca Centro (id fijo)
INSERT INTO sucursal (id, empresa_id, nombre, direccion, archivada)
VALUES ('52525252-0000-0000-0000-000000000002', 'e2e2e2e2-0000-0000-0000-0000000000e2',
        'Cuenca Centro', 'Gran Colombia 7-40, Cuenca', false)
ON CONFLICT (id) DO UPDATE SET nombre = EXCLUDED.nombre;

-- ============================================================================
-- EMPLEADOS (plantados) — usuario + membresía + sucursales + permisos
-- ============================================================================
-- ids fijos
--   ana   aaaaaaaa-...000a   carlos cccccccc-...000c   beto bbbbbbbb-...000b   sofia 55555555-...0055
INSERT INTO usuario (id, empresa_id, email, password_hash, rol, activo, nombre) VALUES
  ('aaaaaaaa-0000-0000-0000-00000000000a', (SELECT empresa_id FROM usuario WHERE email='dueno@costumi.co'), 'ana@ff.ec',    :HASH, 'ENCARGADO', true, 'Ana Torres'),
  ('cccccccc-0000-0000-0000-00000000000c', (SELECT empresa_id FROM usuario WHERE email='dueno@costumi.co'), 'carlos@ff.ec', :HASH, 'MOSTRADOR', true, 'Carlos Andrade'),
  ('bbbbbbbb-0000-0000-0000-00000000000b', (SELECT empresa_id FROM usuario WHERE email='dueno@costumi.co'), 'beto@ff.ec',   :HASH, 'BODEGA',    true, 'Beto Cruz'),
  ('55555555-0000-0000-0000-000000000055', 'e2e2e2e2-0000-0000-0000-0000000000e2', 'sofia@baul.ec', :HASH, 'MOSTRADOR', true, 'Sofía León')
ON CONFLICT (id) DO UPDATE SET password_hash = EXCLUDED.password_hash, rol = EXCLUDED.rol, empresa_id = EXCLUDED.empresa_id;

-- limpiar filas hijas de estos usuarios (idempotencia)
DELETE FROM permiso_empleado WHERE usuario_id IN
  ('aaaaaaaa-0000-0000-0000-00000000000a','cccccccc-0000-0000-0000-00000000000c',
   'bbbbbbbb-0000-0000-0000-00000000000b','55555555-0000-0000-0000-000000000055');
DELETE FROM usuario_sucursal WHERE usuario_id IN
  ('aaaaaaaa-0000-0000-0000-00000000000a','cccccccc-0000-0000-0000-00000000000c',
   'bbbbbbbb-0000-0000-0000-00000000000b','55555555-0000-0000-0000-000000000055',
   'bf2462e1-494c-4934-9d1a-08544894842e','d2d2d2d2-0000-0000-0000-0000000000d2');
DELETE FROM membresia WHERE usuario_id IN
  ('aaaaaaaa-0000-0000-0000-00000000000a','cccccccc-0000-0000-0000-00000000000c',
   'bbbbbbbb-0000-0000-0000-00000000000b','55555555-0000-0000-0000-000000000055');

-- membresías ACTIVAS (rol = el de trabajo). Incluye a los DUEÑOS: sin membresía no pueden volver a
-- «modo trabajo» tras entrar a «comprar» (la Fase B exige membresía activa para el contexto de trabajo).
DELETE FROM membresia WHERE usuario_id IN
  (SELECT id FROM usuario WHERE email IN ('dueno@costumi.co','dueno@baul.ec'));
INSERT INTO membresia (id, usuario_id, empresa_id, rol, estado)
SELECT gen_random_uuid(), u.id, u.empresa_id, u.rol, 'ACTIVA'
FROM usuario u
WHERE u.email IN ('ana@ff.ec','carlos@ff.ec','beto@ff.ec','sofia@baul.ec','dueno@costumi.co','dueno@baul.ec');

-- asignación a sucursales
INSERT INTO usuario_sucursal (id, empresa_id, usuario_id, sucursal_id)
SELECT gen_random_uuid(), s.empresa_id, u.id, s.id
FROM usuario u JOIN sucursal s ON s.empresa_id = u.empresa_id
WHERE (u.email='ana@ff.ec'    AND s.nombre='Cumbayá')
   OR (u.email='carlos@ff.ec' AND s.nombre='Centro Histórico')
   OR (u.email='beto@ff.ec'   AND s.nombre IN ('Centro Histórico','Cumbayá'))
   OR (u.email='sofia@baul.ec' AND s.nombre='Cuenca Centro')
   OR (u.email='dueno@costumi.co' AND s.nombre IN ('Centro Histórico','Cumbayá'))
   OR (u.email='dueno@baul.ec'    AND s.nombre='Cuenca Centro');

-- permisos (overrides) — efectiva = preset del rol ± estos
INSERT INTO permiso_empleado (id, empresa_id, usuario_id, capacidad, concedido)
SELECT gen_random_uuid(), u.empresa_id, u.id, x.capacidad, x.concedido
FROM usuario u
JOIN (VALUES
  ('ana@ff.ec',    'EMPLEADOS_INVITAR',            false),
  ('ana@ff.ec',    'EMPLEADOS_EDITAR_PERMISOS',    false),
  ('ana@ff.ec',    'CONFIGURACION_EDITAR',         false),
  ('carlos@ff.ec', 'REPORTES_VER',                 true),
  ('carlos@ff.ec', 'VENTAS_DEVOLVER',              false),
  ('beto@ff.ec',   'CATALOGO_ETIQUETAS_GESTIONAR', true),
  ('beto@ff.ec',   'INVENTARIO_STOCK_AJUSTAR',     false),
  ('sofia@baul.ec','INVENTARIO_PRENDA_GESTIONAR',  true),
  ('sofia@baul.ec','RENTAS_CERRAR',                false)
) AS x(email, capacidad, concedido) ON x.email = u.email;

-- ---------- verificación ----------
SELECT 'empresas' k, count(*)::text v FROM empresa
UNION ALL SELECT 'sucursales', count(*)::text FROM sucursal
UNION ALL SELECT 'usuarios', count(*)::text FROM usuario
UNION ALL SELECT 'membresias', count(*)::text FROM membresia
UNION ALL SELECT 'usuario_sucursal', count(*)::text FROM usuario_sucursal
UNION ALL SELECT 'permisos', count(*)::text FROM permiso_empleado
UNION ALL SELECT 'configs', count(*)::text FROM configuracion_empresa;
