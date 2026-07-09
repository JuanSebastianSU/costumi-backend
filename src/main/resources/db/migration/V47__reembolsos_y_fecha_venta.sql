-- RF-4.5: política de reembolso por local + fecha de la venta para la ventana de reembolso.

-- Fecha en que se registró la venta (para calcular la ventana de reembolso). Las ventas existentes
-- se marcan con la fecha de la migración (now()); nuevas ventas la fijan al confirmarse.
alter table venta add column creada_en timestamptz not null default now();

-- Política de reembolso por empresa: si se permiten reembolsos y, opcional, la ventana en días desde
-- la venta (0 = sin límite). Defaults que no cambian el comportamiento previo: activos, sin ventana.
alter table configuracion_empresa
    add column reembolsos_activos      boolean not null default true,
    add column ventana_reembolso_dias  integer not null default 0;
