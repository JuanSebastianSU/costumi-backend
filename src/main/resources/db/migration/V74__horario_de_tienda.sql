-- A7/C1: horario de atención de la tienda por día (para mostrar "Abierto · cierra 18:00" / "Cerrado" en la
-- vitrina). Un renglón por día en que la tienda abre; un día sin renglón = cerrado. dia_semana ISO (1=lunes..
-- 7=domingo). Es de la empresa (tenant); su id ES el empresa_id.
create table horario_atencion (
    id          uuid  primary key,
    empresa_id  uuid  not null references empresa (id),
    dia_semana  int   not null check (dia_semana between 1 and 7),
    abre        time  not null,
    cierra      time  not null,
    constraint uq_horario_empresa_dia unique (empresa_id, dia_semana)
);

create index idx_horario_empresa on horario_atencion (empresa_id);
