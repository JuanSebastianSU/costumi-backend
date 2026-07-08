-- Checkout de renta por carrito (RF-16.4/18.6-7): cada artículo del carrito de RENTA lleva su propio
-- periodo (retiro/devolución). Nulos en los carritos de VENTA.
alter table linea_de_carrito add column fecha_retiro date;
alter table linea_de_carrito add column fecha_devolucion date;
