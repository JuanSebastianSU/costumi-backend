-- Recuperación de contraseña (RF-1.1): tokens de un solo uso, con vencimiento.
-- Se guarda solo el HASH del token (SHA-256 hex, 64 chars); el valor en claro viaja por email.
create table token_recuperacion (
    id         uuid         primary key,
    usuario_id uuid         not null references usuario (id),
    token_hash varchar(64)  not null unique,
    expira_en  timestamptz  not null,
    usado      boolean      not null default false
);

create index idx_token_recuperacion_usuario on token_recuperacion (usuario_id);
