package com.costumi.backend.identidad;

import java.util.UUID;

/**
 * Evento de dominio: el SuperAdmin ejecutó una acción de ciclo de vida sobre una Empresa —
 * {@code accion} ∈ {RECHAZADA, SUSPENDIDA, REACTIVADA} (RF-15.3/15.5). API pública de Identidad;
 * Auditoría reacciona para dejar traza (§5.5). La aprobación tiene su propio evento
 * {@link EmpresaAprobada} porque además dispara la provisión de la tienda.
 */
public record EmpresaGestionada(UUID empresaId, String accion) {
}
