-- Módulo Identidad y tenant (RF-15.1): Sucursales de una Empresa.
-- Primera tabla hija de negocio: lleva empresa_id (tenant) con FK a empresa (CLAUDE.md).
create table sucursal (
    id         uuid         primary key,
    empresa_id uuid         not null references empresa (id),
    nombre     varchar(200) not null,
    direccion  varchar(300)
);

-- El filtrado por tenant es por empresa_id: se indexa.
create index idx_sucursal_empresa on sucursal (empresa_id);
