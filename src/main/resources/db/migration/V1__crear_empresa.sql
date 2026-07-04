-- Módulo Identidad y tenant (RF-15): registro de Empresas (tenant).
-- La tabla 'empresa' ES el registro de tenants: su 'id' es el identificador de tenant,
-- por eso NO lleva columna empresa_id (las tablas hijas de negocio sí la llevarán).
create table empresa (
    id             uuid         primary key,
    nombre         varchar(200) not null,
    estado         varchar(20)  not null,
    fecha_registro timestamptz  not null
);
