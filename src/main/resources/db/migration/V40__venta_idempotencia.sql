-- RF-17.6 (offline/outbox): clave de idempotencia para no duplicar ventas por reintentos.
-- Mismo patrón que la renta (V24): índice único parcial por (empresa, clave) cuando la clave existe.
alter table venta add column clave_idempotencia varchar(120);
create unique index ux_venta_empresa_clave on venta (empresa_id, clave_idempotencia)
    where clave_idempotencia is not null;
