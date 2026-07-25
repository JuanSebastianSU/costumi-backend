-- Fase B: membresía persona↔tienda. Hoy un usuario tiene UN rol + UNA empresa fijos (columna en `usuario`).
-- Esta tabla es ADITIVA: modela que una persona pueda pertenecer a varias tiendas, cada una con su rol.
-- No se toca `usuario` (el login/token siguen usando su empresa+rol "base"); el cambio de contexto emite un
-- token con la empresa+rol de la membresía elegida.
create table membresia (
    id          uuid          primary key,
    usuario_id  uuid          not null references usuario (id),
    empresa_id  uuid          not null references empresa (id),
    rol         varchar(20)   not null,
    estado      varchar(20)   not null default 'ACTIVA',
    constraint uq_membresia_usuario_empresa unique (usuario_id, empresa_id)
);

create index idx_membresia_usuario on membresia (usuario_id);

-- Backfill: cada usuario que ya pertenece a una empresa con un rol de staff queda con su membresía ACTIVA.
-- Se excluyen SUPERADMIN (plataforma) y CLIENTE (no son miembros de una tienda).
insert into membresia (id, usuario_id, empresa_id, rol, estado)
select gen_random_uuid(), u.id, u.empresa_id, u.rol, 'ACTIVA'
from usuario u
where u.empresa_id is not null and u.rol not in ('SUPERADMIN', 'CLIENTE');
