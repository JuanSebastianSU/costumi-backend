-- Fase B (paso 2, H1): regla de seguridad #2 — una persona tiene a lo sumo UNA membresía de trabajo ACTIVA
-- (no se puede trabajar para dos tiendas a la vez). Se garantiza con un índice único PARCIAL sobre el usuario
-- restringido a las filas ACTIVA. Las membresías SUSPENDIDA/históricas no cuentan, así que puede haber varias.
-- Seguro sobre los datos actuales: el backfill de V76 creó una sola membresía ACTIVA por usuario staff.
create unique index uq_membresia_una_activa on membresia (usuario_id) where estado = 'ACTIVA';
