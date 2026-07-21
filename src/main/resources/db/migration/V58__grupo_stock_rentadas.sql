-- Estado RENTADA en los grupos de stock (RF-2.2/9.3): unidades comprometidas en una renta (fuera del
-- local pero aún propiedad de la empresa). Rentar mueve disponible -> rentada (bajan los disponibles sin
-- cambiar el total); devolver la vuelve a disponible (o a dañada/perdida). Vender sigue dando de baja.
alter table grupo_de_stock add column rentadas integer not null default 0;
