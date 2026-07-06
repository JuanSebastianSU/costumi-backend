-- Caja (RF-6.3/6.10): turno con fondo inicial + movimientos por método de pago; corte y cuadre al cierre.
create table turno_caja (
    id               uuid         primary key,
    empresa_id       uuid         not null references empresa (id),
    sucursal_id      uuid         not null references sucursal (id),
    empleado_id      uuid         not null references usuario (id),
    fondo_inicial    numeric(12, 2) not null,
    estado           varchar(8)   not null,
    efectivo_contado numeric(12, 2)
);

create index idx_turno_caja_empresa on turno_caja (empresa_id);

create table movimiento_caja (
    id         uuid           primary key,
    turno_id   uuid           not null references turno_caja (id) on delete cascade,
    empresa_id uuid           not null references empresa (id),
    tipo       varchar(8)     not null,
    concepto   varchar(200)   not null,
    monto      numeric(12, 2) not null,
    metodo     varchar(14)    not null,
    orden      integer        not null
);

create index idx_movimiento_caja_turno on movimiento_caja (turno_id);
