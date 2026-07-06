-- RF-17.6 (offline/outbox): clave de idempotencia para no duplicar rentas por reintentos.
alter table renta add column clave_idempotencia varchar(120);
create unique index ux_renta_empresa_clave on renta (empresa_id, clave_idempotencia)
    where clave_idempotencia is not null;
