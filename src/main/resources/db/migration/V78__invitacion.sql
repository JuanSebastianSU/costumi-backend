-- Fase B (paso 3): invitación de trabajo por email. Entidad aparte de membresia porque al invitar la persona
-- puede no tener cuenta aún (se crea al aceptar). Token de un solo uso (se guarda el hash) con expiración, y
-- registro de aceptación de T&C. Las membresías nuevas se crean ACTIVA al aceptar; la invitación no las modela.
create table invitacion (
    id                 uuid          primary key,
    empresa_id         uuid          not null references empresa (id),
    email              varchar(320)  not null,
    rol                varchar(20)   not null,
    token_hash         varchar(64)   not null unique,
    expira_en          timestamptz   not null,
    estado             varchar(20)   not null default 'PENDIENTE',
    acepto_terminos_en timestamptz
);
create index idx_invitacion_empresa on invitacion (empresa_id);
-- Solo una invitación PENDIENTE por email dentro de una empresa (re-invitar reemplaza la anterior).
create unique index uq_invitacion_pendiente on invitacion (empresa_id, lower(email)) where estado = 'PENDIENTE';

-- Sucursales asignadas al invitar (se aplican al aceptar). Hijo de invitacion.
create table invitacion_sucursal (
    invitacion_id uuid not null references invitacion (id) on delete cascade,
    sucursal_id   uuid not null references sucursal (id),
    primary key (invitacion_id, sucursal_id)
);
