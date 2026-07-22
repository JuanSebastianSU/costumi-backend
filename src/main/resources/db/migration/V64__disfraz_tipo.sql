-- Tipo del disfraz (RF-2.3): el DUEÑO decide para qué está disponible cada disfraz — solo RENTA, solo
-- VENTA o AMBOS. Acota lo que el cliente puede hacer (no se puede comprar un disfraz de solo renta).
-- Los existentes quedan en AMBOS para no cambiar su comportamiento actual.
alter table disfraz add column tipo varchar(10) not null default 'AMBOS';
