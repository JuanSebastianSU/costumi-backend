-- Reset del CATÁLOGO + OPERACIONES (conserva identidad/config/sucursales/empleados).
-- Útil para re-correr 02_catalogo.mjs / 03_operaciones.mjs desde limpio.
DO $$
DECLARE lista text;
BEGIN
  SELECT string_agg(format('%I', tablename), ', ') INTO lista
  FROM pg_tables
  WHERE schemaname = 'public'
    AND tablename NOT IN (
      'flyway_schema_history',
      'empresa','configuracion_empresa','sucursal',
      'usuario','membresia','usuario_sucursal','permiso_empleado',
      'token_refresh','token_recuperacion','invitacion','invitacion_sucursal');
  EXECUTE 'TRUNCATE TABLE ' || lista || ' RESTART IDENTITY CASCADE';
  RAISE NOTICE 'Catálogo/operaciones truncados (identidad conservada).';
END $$;
