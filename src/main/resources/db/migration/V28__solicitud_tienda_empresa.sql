-- Solicitud de tienda (marketplace): la empresa PENDIENTE guarda los datos que cargó el cliente
-- y el id del cliente solicitante, para que el SuperAdmin sepa a quién promover a Dueño al aprobar.
-- Columnas opcionales (el auto-registro clásico no las llena).
ALTER TABLE empresa ADD COLUMN ubicacion VARCHAR(300);
ALTER TABLE empresa ADD COLUMN contacto VARCHAR(200);
ALTER TABLE empresa ADD COLUMN solicitante_id UUID;
