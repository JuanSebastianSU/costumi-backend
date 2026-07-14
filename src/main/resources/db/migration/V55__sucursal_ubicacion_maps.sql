-- Ubicación de Google Maps de la sucursal (RF-11/15.1): un enlace que la tienda pega desde Google
-- Maps, para incluirlo en los mensajes automáticos ({maps}) junto con la dirección de texto ({direccion}).
alter table sucursal add column ubicacion_maps varchar(500);
