-- Fecha de creación de la reserva (para expirarla a las 24 h si no se paga). Se setea en la BD por
-- defecto al insertar, así no hay que tocar el dominio ni el mapeo JPA de la renta. Las filas
-- existentes toman la fecha de la migración (ya están entregadas/cerradas, no son reservas vivas).
alter table renta add column creada_en timestamptz not null default now();
