-- ============================================================================
-- SEED 04 — Backdating: reparte las fechas de registro en los últimos ~30 días
-- para que Reportes (ingresos/ventas/rentas por día, rankings) tengan serie.
-- No toca fecha_retiro/fecha_devolucion de rentas (definen la lógica de estado),
-- ni el turno de caja ABIERTO de hoy.
-- ============================================================================
\set ON_ERROR_STOP on

UPDATE venta
   SET creada_en = now() - (floor(random()*30))::int * interval '1 day'
                         - (floor(random()*10))::int * interval '1 hour';

UPDATE renta
   SET creada_en = now() - (floor(random()*30))::int * interval '1 day';

UPDATE pago
   SET fecha = now() - (floor(random()*30))::int * interval '1 day'
                     - (floor(random()*10))::int * interval '1 hour';

UPDATE solicitud_reembolso
   SET creada_en = now() - (floor(random()*25))::int * interval '1 day',
       decidida_en = CASE WHEN decidida_en IS NOT NULL
                          THEN now() - (floor(random()*20))::int * interval '1 day' END;

UPDATE devolucion
   SET registrada_en = now() - (floor(random()*20))::int * interval '1 day';

-- turnos de caja CERRADOS al pasado; el ABIERTO de hoy se queda hoy
UPDATE turno_caja
   SET abierto_en = now() - (floor(random()*30) + 1)::int * interval '1 day',
       cerrado_en = cerrado_en - interval '0 day'
 WHERE cerrado_en IS NOT NULL;

SELECT 'ventas' k, count(*) v, min(creada_en)::date desde, max(creada_en)::date hasta FROM venta
UNION ALL SELECT 'rentas', count(*), min(creada_en)::date, max(creada_en)::date FROM renta
UNION ALL SELECT 'pagos',  count(*), min(fecha)::date, max(fecha)::date FROM pago;
