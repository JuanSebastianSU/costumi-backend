-- Categorías de DISFRAZ (RF-2.3): taxonomía propia del dueño para agrupar sus disfraces (ej. "Piratas"),
-- SEPARADA de las categorías de prenda (Camisa, Pantalón…). Son dos conceptos distintos.
create table categoria_disfraz (
    id         uuid         primary key,
    empresa_id uuid         not null references empresa (id),
    nombre     varchar(120) not null,
    archivada  boolean      not null default false
);

create index idx_categoria_disfraz_empresa on categoria_disfraz (empresa_id);

-- Repunta disfraz.categoria_id: en V60 apuntaba (por error) a las categorías de PRENDA; ahora a las de
-- DISFRAZ. Los valores existentes referenciaban categorías de prenda, así que se limpian.
alter table disfraz drop constraint if exists disfraz_categoria_id_fkey;
update disfraz set categoria_id = null;
alter table disfraz add constraint disfraz_categoria_id_fkey
    foreign key (categoria_id) references categoria_disfraz (id);
