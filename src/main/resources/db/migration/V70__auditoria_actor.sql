-- A3-bis (RF-0.5/15.5): la auditoría deja constancia de QUIÉN realizó la acción.
-- Hasta ahora el registro guardaba qué/cuándo pero no el actor. Se añade el id del usuario
-- autenticado que disparó la acción. Nullable: las acciones de sistema/plataforma (jobs, o
-- lo ya registrado antes de esta columna) no tienen actor y quedan en NULL.
alter table registro_auditoria
    add column actor_usuario_id uuid;
