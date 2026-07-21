-- Precio de VENTA general del disfraz (RF-2.10): el dueño fija el precio final de venta del conjunto,
-- que anula la suma de venta por prendas (paralelo a precio_renta_general, que ya existía para renta).
-- Nullable: sin él, la venta se cobra por la suma de sus piezas.
alter table disfraz add column precio_venta_general numeric(12, 2);
