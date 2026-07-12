-- Solicitudes de reembolso (RF-4.5/6.9): proceso de dos pasos. El cliente (registrado por el personal)
-- solicita con un motivo; la sucursal aprueba/rechaza dejando un motivo. La aprobación mueve el dinero
-- (asiento REEMBOLSO y, si el pago fue con tarjeta, refund en la pasarela) y exige que el ítem ya esté devuelto.
create table solicitud_reembolso (
    id                     uuid         primary key,
    empresa_id             uuid         not null references empresa (id),
    tipo_concepto          varchar(10)  not null,          -- VENTA | RENTA
    concepto_id            uuid         not null,          -- id de la venta o renta
    solicitante_cliente_id uuid,                           -- cliente al que se reembolsaría (opcional)
    monto                  numeric(12,2) not null,
    motivo_solicitud       varchar(500) not null,
    estado                 varchar(12)  not null,          -- PENDIENTE | APROBADA | RECHAZADA
    motivo_decision        varchar(500),
    decidido_por_usuario_id uuid,
    rol_decision           varchar(20),
    creada_en              timestamptz  not null,
    decidida_en            timestamptz
);

create index idx_solicitud_reembolso_empresa  on solicitud_reembolso (empresa_id);
create index idx_solicitud_reembolso_concepto on solicitud_reembolso (empresa_id, concepto_id);
