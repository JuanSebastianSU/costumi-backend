-- Perfil propio (RF-14): hasta ahora el usuario no tenía dónde poner su nombre ni su teléfono, así que
-- la pantalla "Perfil" del cliente solo podía mostrar el correo, y la ficha que se le crea al comprar en
-- una tienda quedaba nombrada con su email.
--
-- El nombre y el teléfono viven en el usuario (son suyos, valen para todas las tiendas), no en la ficha
-- de cliente, que es por empresa y la administra el dueño.
alter table usuario
    add column nombre   varchar(120),
    add column telefono varchar(40);
