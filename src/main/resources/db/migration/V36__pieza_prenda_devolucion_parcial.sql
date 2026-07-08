-- Devolución parcial (RF-5.5/5.6): cada pieza del checklist se liga a la prenda (artículo) concreta
-- de la renta, para atribuir el daño/pérdida al grupo de stock correcto y contar lo ya devuelto.
alter table pieza_revisada add column prenda_id uuid;

-- Backfill: las piezas existentes se atribuyen a la prenda principal de la renta de su devolución.
update pieza_revisada p
set prenda_id = (
    select r.prenda_id from devolucion d join renta r on r.id = d.renta_id where d.id = p.devolucion_id
)
where p.prenda_id is null;

alter table pieza_revisada alter column prenda_id set not null;

create index idx_pieza_prenda on pieza_revisada (empresa_id, prenda_id);
