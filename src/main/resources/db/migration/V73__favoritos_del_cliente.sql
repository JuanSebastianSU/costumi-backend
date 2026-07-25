-- C4: favoritos ("Mis guardados") del cliente del marketplace, sincronizados con la cuenta (antes solo
-- vivían en Room, local a cada dispositivo). Son del USUARIO (cruzan tiendas, como su historial), no de una
-- empresa: por eso NO llevan filtro multi-tenant. Guardan un snapshot del disfraz para pintar la lista sin
-- resolverlo en cada dispositivo. Único por (usuario, disfraz): re-guardar actualiza, no duplica.
create table favorito_disfraz (
    id           uuid           primary key,
    usuario_id   uuid           not null,
    disfraz_id   uuid           not null,
    empresa_id   uuid           not null,
    nombre       varchar(200)   not null,
    foto_url     varchar(500),
    precio_renta numeric(12, 2),
    precio_venta numeric(12, 2),
    guardado_en  timestamptz    not null,
    constraint uq_favorito_usuario_disfraz unique (usuario_id, disfraz_id)
);

create index idx_favorito_usuario on favorito_disfraz (usuario_id, guardado_en desc);
