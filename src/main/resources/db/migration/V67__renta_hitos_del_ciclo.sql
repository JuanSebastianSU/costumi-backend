-- Hitos REALES del ciclo de vida de la renta (RF-3.5), para mostrar en la app "cuándo se entregó, cuándo
-- se devolvió y cuándo se cerró" (además de creada_en = cuándo se registró). Son nullable: una renta
-- RESERVADA todavía no tiene ninguno; se setean en cada transición de estado. Las filas existentes quedan
-- en null (no se puede reconstruir el instante pasado; la app degrada mostrando solo lo que hay).
alter table renta add column entregada_en     timestamptz;
alter table renta add column devuelta_real_en timestamptz;
alter table renta add column cerrada_en       timestamptz;
