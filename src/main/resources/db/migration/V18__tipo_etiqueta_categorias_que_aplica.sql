-- RF-2.7.2: a qué categorías aplica un tipo de etiqueta. Sin filas para un tipo = aplica a TODAS
-- las categorías (dimensión global, p. ej. "Color"). Con filas = solo esas categorías.
create table tipo_etiqueta_categoria (
    tipo_etiqueta_id uuid not null references tipo_etiqueta (id) on delete cascade,
    categoria_id     uuid not null references categoria (id),
    primary key (tipo_etiqueta_id, categoria_id)
);

create index idx_tipo_etiqueta_categoria_tipo on tipo_etiqueta_categoria (tipo_etiqueta_id);
