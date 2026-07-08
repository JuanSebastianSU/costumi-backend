-- Renta multi-artículo (RF-3.1/3.6/16.2): una renta puede llevar varios artículos, cada uno con su
-- cantidad y precio por día. El detalle vive en renta_linea; la cabecera renta conserva el artículo
-- principal (prenda_id/precio_por_dia) de forma denormalizada para las vistas por una sola prenda
-- (rentas vencidas, devolución, contrato).
create table renta_linea (
    id             uuid           primary key,
    renta_id       uuid           not null references renta (id),
    empresa_id     uuid           not null references empresa (id),
    prenda_id      uuid           not null references prenda (id),
    cantidad       integer        not null,
    precio_por_dia numeric(12, 2) not null
);

-- Backfill: cada renta existente pasa a tener una línea (cantidad 1) con su artículo principal.
-- El id de la línea reutiliza el id de la renta (1:1, evita depender de un generador de uuid en SQL).
insert into renta_linea (id, renta_id, empresa_id, prenda_id, cantidad, precio_por_dia)
select r.id, r.id, r.empresa_id, r.prenda_id, 1, r.precio_por_dia
from renta r;

create index idx_renta_linea_renta on renta_linea (renta_id);
create index idx_renta_linea_prenda on renta_linea (empresa_id, prenda_id);
