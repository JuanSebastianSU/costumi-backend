-- Módulo Clientes (RF-7): ficha del cliente por empresa.
create table cliente (
    id             uuid         primary key,
    empresa_id     uuid         not null references empresa (id),
    nombre         varchar(200) not null,
    telefono       varchar(40),
    email          varchar(200),
    documento      varchar(60),
    direccion      varchar(300),
    en_lista_negra boolean      not null default false
);

create index idx_cliente_empresa on cliente (empresa_id);
create index idx_cliente_documento on cliente (empresa_id, documento);
