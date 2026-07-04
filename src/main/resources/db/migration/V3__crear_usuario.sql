-- Módulo Identidad y tenant (RF-1, RF-17.4): usuarios del sistema.
-- empresa_id es NULO para el SuperAdmin (usuario de plataforma); para el resto, su tenant.
create table usuario (
    id            uuid         primary key,
    empresa_id    uuid         references empresa (id),
    email         varchar(320) not null unique,
    password_hash varchar(100) not null,
    rol           varchar(20)  not null
);

create index idx_usuario_empresa on usuario (empresa_id);
