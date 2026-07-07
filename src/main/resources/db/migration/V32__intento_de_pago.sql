-- Pago en línea (RF-6.11): intentos de pago iniciados en la pasarela. Al confirmar el webhook, se
-- registra el Pago correspondiente (idempotente) y el intento pasa a CONFIRMADO.
create table intento_de_pago (
    id                 uuid          primary key,
    empresa_id         uuid          not null references empresa (id),
    sucursal_id        uuid          not null,
    empleado_id        uuid          not null,
    tipo_concepto      varchar(10)   not null,
    concepto_id        uuid          not null,
    monto              numeric(12, 2) not null,
    moneda             varchar(8)    not null,
    referencia_externa varchar(200),
    estado             varchar(12)   not null,
    fecha              timestamptz   not null
);

create index idx_intento_pago_empresa on intento_de_pago (empresa_id);
