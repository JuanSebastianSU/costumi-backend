-- Archivado de fichas de cliente (R-E): soft-delete para retirarlas de la lista activa sin borrar su
-- historial. El personal no puede rentar/vender a una ficha archivada (se reactiva primero); el marketplace no la consulta.
alter table cliente add column archivada boolean not null default false;
