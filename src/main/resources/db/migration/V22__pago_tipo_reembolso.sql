-- RF-6.9: un pago puede ser COBRO o REEMBOLSO (para saldos y devoluciones de dinero).
alter table pago add column tipo_pago varchar(10) not null default 'COBRO';
