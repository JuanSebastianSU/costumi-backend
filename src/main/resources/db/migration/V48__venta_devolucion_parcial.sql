-- RF-4.5: reembolso PARCIAL de venta. Cada línea lleva cuántas unidades ya se devolvieron, y la venta
-- puede quedar en estado PARCIALMENTE_DEVUELTA (que no cabe en varchar(12) -> se amplía la columna).
alter table linea_de_venta add column cantidad_devuelta integer not null default 0;
alter table venta alter column estado type varchar(24);
