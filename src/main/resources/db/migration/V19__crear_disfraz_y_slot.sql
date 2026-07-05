-- Capa 3 (RF-2.3/2.4): Disfraz (modo unidad-fija / por-partes) con sus slots (hasta 8) y, por slot
-- personalizable, el pool de etiquetas permitidas. La disponibilidad NO se guarda: se deriva (RF-2.4).
create table disfraz (
    id             uuid         primary key,
    empresa_id     uuid         not null references empresa (id),
    nombre         varchar(160) not null,
    modo           varchar(12)  not null,
    prenda_fija_id uuid         references prenda (id)
);

create index idx_disfraz_empresa on disfraz (empresa_id);

create table disfraz_slot (
    id             uuid         primary key,
    disfraz_id     uuid         not null references disfraz (id) on delete cascade,
    orden          integer      not null,
    nombre         varchar(120) not null,
    eje_talla      varchar(8)   not null,
    talla_fija     varchar(60),
    eje_prenda     varchar(16)  not null,
    prenda_fija_id uuid         references prenda (id),
    categoria_id   uuid         references categoria (id),
    opcional       boolean      not null default false
);

create index idx_disfraz_slot_disfraz on disfraz_slot (disfraz_id);

-- Pool de un slot personalizable: valores de etiqueta permitidos por dimensión.
create table disfraz_slot_etiqueta (
    slot_id           uuid not null references disfraz_slot (id) on delete cascade,
    tipo_etiqueta_id  uuid not null references tipo_etiqueta (id),
    valor_etiqueta_id uuid not null references valor_etiqueta (id),
    primary key (slot_id, tipo_etiqueta_id, valor_etiqueta_id)
);
