-- A7 (media e identidad): la tienda tiene logo/portada, descripción y ciudad para su vitrina; el usuario
-- (cliente) tiene foto de perfil. Todo opcional (nullable) — las tiendas/usuarios ya existentes no la tienen.
alter table empresa
    add column descripcion varchar(1000),
    add column ciudad      varchar(120),
    add column logo_url    varchar(500),
    add column portada_url varchar(500);

alter table usuario
    add column foto_url varchar(500);
