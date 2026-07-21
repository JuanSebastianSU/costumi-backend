-- Opciones de prenda EXPLÍCITAS de un slot personalizable (RF-2.3): el dueño elige a mano las prendas
-- concretas del inventario que son las opciones de esa parte (alternativa al pool por categoría+etiquetas).
-- Son referencias a prendas existentes; el stock se sigue llevando en la prenda (no cambia el inventario).
create table disfraz_slot_prenda_opcion (
    slot_id   uuid not null references disfraz_slot (id) on delete cascade,
    prenda_id uuid not null references prenda (id),
    primary key (slot_id, prenda_id)
);
