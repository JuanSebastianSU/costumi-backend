-- Fechas de la devolución (G10). `registrada_en`: cuándo se registró la devolución, para ordenar la lista
-- por recencia (más reciente primero); las filas existentes toman la fecha de la migración (default now()).
-- `fecha_devolucion_real`: la fecha real en que el cliente devolvió, que hoy se recibe en el request para
-- calcular el recargo por retraso pero se descartaba; nullable porque las filas viejas no la guardaron.
alter table devolucion add column registrada_en timestamptz not null default now();
alter table devolucion add column fecha_devolucion_real date;
