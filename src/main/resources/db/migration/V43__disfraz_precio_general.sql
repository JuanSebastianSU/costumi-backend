-- RF-2.10: precio de renta general del disfraz (por día). Opcional; si está, anula la suma por prendas
-- y se cobra ese valor por el conjunto. Nulo = el precio se deriva de las prendas que lo componen.
alter table disfraz add column precio_renta_general numeric(12, 2);
