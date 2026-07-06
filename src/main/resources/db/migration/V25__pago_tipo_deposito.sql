-- RF-6.2/6.8: el depósito/garantía se rastrea como pago aparte (retención, no ingreso) y su
-- devolución del remanente lo libera. Los nuevos valores de tipo_pago (DEPOSITO,
-- DEVOLUCION_DEPOSITO) no caben en varchar(10), así que se amplía la columna.
alter table pago alter column tipo_pago type varchar(20);
