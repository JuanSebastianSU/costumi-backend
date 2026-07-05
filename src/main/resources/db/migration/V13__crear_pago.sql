-- Módulo Pagos/caja (RF-6): pagos ligados a renta/venta, con método e idempotencia.
create table pago (
    id                 uuid           primary key,
    empresa_id         uuid           not null references empresa (id),
    sucursal_id        uuid           not null references sucursal (id),
    empleado_id        uuid           not null references usuario (id),
    tipo_concepto      varchar(10)    not null,
    concepto_id        uuid           not null,
    monto              numeric(12, 2) not null,
    metodo             varchar(15)    not null,
    referencia         varchar(120),
    fecha              timestamptz    not null,
    clave_idempotencia varchar(120)
);

create index idx_pago_concepto on pago (empresa_id, concepto_id);

-- Idempotencia: no se duplica un pago con la misma clave dentro de la empresa (RF-17.6).
create unique index ux_pago_idempotencia
    on pago (empresa_id, clave_idempotencia) where clave_idempotencia is not null;
