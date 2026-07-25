-- A7/G17: la sucursal (punto físico) tiene descripción, coordenadas para el mapa y una foto, para que la
-- vitrina del marketplace muestre dónde retirar. Todo opcional (nullable) — las sucursales ya existentes
-- no lo tienen.
alter table sucursal
    add column descripcion varchar(1000),
    add column latitud     double precision,
    add column longitud    double precision,
    add column foto_url     varchar(500);
