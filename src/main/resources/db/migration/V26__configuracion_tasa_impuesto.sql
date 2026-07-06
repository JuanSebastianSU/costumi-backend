-- RF-6.5/12.2: tasa de impuesto configurable por empresa (precios impuesto-incluido; el desglose se
-- calcula en el comprobante). Por defecto 0 = sin impuesto, para no cambiar el comportamiento previo.
alter table configuracion_empresa
    add column tasa_impuesto numeric(5, 4) not null default 0;
