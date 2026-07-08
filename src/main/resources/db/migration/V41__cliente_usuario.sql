-- RF-14.4/18.5: enlaza la ficha de cliente con el usuario del marketplace. La cuenta vive a nivel
-- plataforma (usuario) y cada tienda tiene su proyección (ficha de cliente). Una ficha por (empresa, usuario).
alter table cliente add column usuario_id uuid;

create unique index ux_cliente_empresa_usuario on cliente (empresa_id, usuario_id)
    where usuario_id is not null;
