-- RF-2.3: el disfraz es SIEMPRE un conjunto de prendas (1..8 slots). Se elimina el modo UNIDAD_FIJA
-- (una "pieza única" = un disfraz con un único slot fijo) y la talla deja de ser un eje del slot: se
-- modela como una etiqueta dentro del pool. Además se agrega el estado archivado/activo del disfraz.

-- 1) La talla ya no es un eje del slot (se representa como etiqueta en el pool): se quitan sus columnas.
alter table disfraz_slot drop column eje_talla;
alter table disfraz_slot drop column talla_fija;

-- 2) Cada disfraz UNIDAD_FIJA existente se convierte en un disfraz por-partes con un único slot fijo.
insert into disfraz_slot (id, disfraz_id, orden, nombre, eje_prenda, prenda_fija_id, categoria_id, opcional)
select gen_random_uuid(), d.id, 1, d.nombre, 'FIJA', d.prenda_fija_id, null, false
from disfraz d
where d.modo = 'UNIDAD_FIJA';

-- 3) El disfraz ya no lleva modo ni prenda fija propia: su estructura vive en los slots.
alter table disfraz drop column modo;
alter table disfraz drop column prenda_fija_id;

-- 4) Estado del disfraz: activo por defecto; archivar lo retira de la vitrina sin borrarlo.
alter table disfraz add column activo boolean not null default true;
