-- Categoría del disfraz (RF-2.3): el dueño agrupa/ve sus disfraces por categoría (ej. "Piratas"),
-- igual que las prendas. Nullable: los disfraces existentes no tenían categoría.
alter table disfraz add column categoria_id uuid references categoria (id);

create index idx_disfraz_categoria on disfraz (categoria_id);
