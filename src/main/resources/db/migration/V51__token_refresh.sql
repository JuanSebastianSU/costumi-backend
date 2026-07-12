-- Registro server-side de los tokens de refresco (C2): permite rotación, revocación y detección de reuso.
-- El jti es el identificador del JWT; familia_id agrupa la cadena de rotaciones de una misma sesión.
-- Estados: ACTIVO (vigente), ROTADO (ya usado para rotar), REVOCADO (logout o revocación por reuso).
create table token_refresh (
    jti        uuid         primary key,
    usuario_id uuid         not null references usuario (id),
    familia_id uuid         not null,
    estado     varchar(20)  not null,
    expira_en  timestamptz  not null,
    creado_en  timestamptz  not null
);

create index idx_token_refresh_familia on token_refresh (familia_id);
create index idx_token_refresh_usuario on token_refresh (usuario_id);
