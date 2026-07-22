-- Slice 4 (RF-16): el carrito del cliente ahora acepta DISFRACES además de prendas. Una línea es
-- de una prenda (prenda_id) O de un disfraz (disfraz_id), nunca ambos ni ninguno. La línea de
-- disfraz guarda la elección de prenda por slot personalizable en una tabla hija.
alter table linea_de_carrito alter column prenda_id drop not null;
alter table linea_de_carrito add column disfraz_id uuid references disfraz (id);
alter table linea_de_carrito add constraint ck_linea_carrito_prenda_xor_disfraz
    check ((prenda_id is not null) <> (disfraz_id is not null));

-- Elección concreta de prenda para un slot personalizable del disfraz agregado (una fila por slot).
create table linea_de_carrito_seleccion (
    id         uuid    primary key,
    linea_id   uuid    not null references linea_de_carrito (id) on delete cascade,
    empresa_id uuid    not null references empresa (id),
    orden      integer not null,
    prenda_id  uuid    not null references prenda (id)
);

create index idx_linea_carrito_sel on linea_de_carrito_seleccion (linea_id);
