-- El disfraz sobrevive al cobro (RF-2.3): hasta ahora, al confirmar una venta o una renta el disfraz se
-- resolvía a sus prendas y se perdía — el cliente veía "Capa Real" cuando había comprado un
-- "Traje Pirata", y el dueño no podía saber qué DISFRAZ se vende más (solo qué prenda).
--
-- Cada línea recuerda ahora de qué disfraz salió:
--   disfraz_id        el disfraz (null si la línea es una prenda suelta, que es lo normal),
--   disfraz_grupo     una instancia concreta de ese disfraz dentro del pedido. Hace falta porque el
--                     mismo disfraz puede ir dos veces con piezas distintas: sin el grupo, los dos se
--                     mezclarían en uno solo al agrupar,
--   disfraz_cantidad  cuántos disfraces de ese grupo se cobraron (las líneas ya traen la cantidad de
--                     prendas, que es cantidad_de_disfraces × piezas),
--   disfraz_nombre    el nombre CON EL QUE SE COBRÓ. Se guarda (no se resuelve al leer) por dos razones:
--                     un pedido histórico no debe cambiar si después renombran el disfraz, y así los
--                     módulos de ventas/rentas no dependen del de disfraces (evita un ciclo).
alter table linea_de_venta
    add column disfraz_id       uuid references disfraz (id),
    add column disfraz_grupo    uuid,
    add column disfraz_cantidad integer,
    add column disfraz_nombre   varchar(120);

alter table renta_linea
    add column disfraz_id       uuid references disfraz (id),
    add column disfraz_grupo    uuid,
    add column disfraz_cantidad integer,
    add column disfraz_nombre   varchar(120);

-- Las tres viajan juntas: o la línea viene de un disfraz, o no.
alter table linea_de_venta
    add constraint chk_linea_venta_origen_disfraz
        check ((disfraz_id is null and disfraz_grupo is null and disfraz_cantidad is null)
            or (disfraz_id is not null and disfraz_grupo is not null and disfraz_cantidad > 0));

alter table renta_linea
    add constraint chk_renta_linea_origen_disfraz
        check ((disfraz_id is null and disfraz_grupo is null and disfraz_cantidad is null)
            or (disfraz_id is not null and disfraz_grupo is not null and disfraz_cantidad > 0));

create index idx_linea_venta_disfraz on linea_de_venta (empresa_id, disfraz_id);
create index idx_renta_linea_disfraz on renta_linea (empresa_id, disfraz_id);
