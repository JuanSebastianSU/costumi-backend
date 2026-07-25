-- Timestamps del turno de caja (G14): cuándo se abrió y cuándo se cerró, para mostrar "cuánto lleva
-- abierto" y fechar el corte. Nullable: los turnos previos a esta columna no se pueden reconstruir; se
-- setean al abrir/cerrar de aquí en más.
alter table turno_caja add column abierto_en timestamptz;
alter table turno_caja add column cerrado_en timestamptz;
