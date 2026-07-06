-- RF-2.10: la Prenda gana costo de adquisición (para el margen en reportes) y depósito sugerido.
alter table prenda add column costo_adquisicion numeric(12, 2);
alter table prenda add column deposito_sugerido numeric(12, 2);
