-- Stock por sucursal (RF-18.2/10): el grupo de stock pertenece a una sucursal concreta, para que la
-- disponibilidad sea por local y habilitar transferencias entre sucursales.
alter table grupo_de_stock add column sucursal_id uuid;

-- Backfill: los grupos existentes se asignan a la primera sucursal de su empresa (p. ej. "Casa Matriz").
update grupo_de_stock g
set sucursal_id = (
    select s.id from sucursal s where s.empresa_id = g.empresa_id order by s.nombre, s.id limit 1
)
where g.sucursal_id is null;

alter table grupo_de_stock alter column sucursal_id set not null;

create index idx_grupo_stock_sucursal on grupo_de_stock (sucursal_id);
