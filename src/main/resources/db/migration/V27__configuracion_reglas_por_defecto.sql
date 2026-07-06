-- RF-12.2: reglas por defecto configurables por empresa — moneda y recargo por retraso por día.
-- Defaults que no cambian el comportamiento previo: moneda COP, recargo 0.
alter table configuracion_empresa
    add column moneda              varchar(3)      not null default 'COP',
    add column recargo_retraso_dia numeric(12, 2)  not null default 0;
