-- RF-5.2/12.2: el dueño elige cómo se cobra el recargo por retraso: ACUMULATIVA (monto × días) o FIJA
-- (monto único). Default ACUMULATIVA: no cambia el comportamiento previo.
alter table configuracion_empresa
    add column modo_recargo_retraso varchar(12) not null default 'ACUMULATIVA';
