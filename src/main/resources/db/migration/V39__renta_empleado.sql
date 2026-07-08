-- RF-1.4: toda transacción queda asociada al usuario logueado que la realizó. La renta registra el
-- empleado/usuario que la creó (nulo en rentas previas a esta columna).
alter table renta add column empleado_id uuid references usuario (id);

create index idx_renta_empleado on renta (empresa_id, empleado_id);
