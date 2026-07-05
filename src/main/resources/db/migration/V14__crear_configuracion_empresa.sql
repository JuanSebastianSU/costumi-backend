-- Módulo Configuración de empresa (RF-12.4): interruptores de módulos, uno por empresa.
create table configuracion_empresa (
    empresa_id     uuid    primary key references empresa (id),
    conteo_stock   boolean not null,
    multas_activo  boolean not null,
    multi_sucursal boolean not null,
    pago_en_linea  boolean not null
);
