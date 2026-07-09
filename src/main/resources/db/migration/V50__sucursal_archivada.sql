-- R-C: ciclo de vida de la Sucursal (RF-15.1). Una sucursal puede archivarse (retirarse de la
-- operación sin borrarla, conservando su historial) y reactivarse. Las sucursales existentes nacen activas.
alter table sucursal add column archivada boolean not null default false;
