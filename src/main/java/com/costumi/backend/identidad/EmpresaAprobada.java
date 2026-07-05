package com.costumi.backend.identidad;

import java.util.UUID;

/**
 * Evento de dominio: una Empresa fue <b>aprobada</b> por el SuperAdmin (RF-15.3) y pasa a operar.
 * Es API pública del módulo Identidad; otros módulos reaccionan (p. ej. Catálogo siembra la taxonomía
 * básica, RF-2.7.7) sin que Identidad los conozca (§5.5).
 */
public record EmpresaAprobada(UUID empresaId) {
}
