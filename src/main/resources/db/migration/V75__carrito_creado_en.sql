-- A7/C5: cuándo se creó el carrito, para "Mis carritos" (ordenar y mostrar "hace 2 días"). Se setea por
-- defecto en la BD para los carritos ya existentes y los nuevos, sin tocar el dominio.
alter table carrito
    add column creado_en timestamptz not null default now();
