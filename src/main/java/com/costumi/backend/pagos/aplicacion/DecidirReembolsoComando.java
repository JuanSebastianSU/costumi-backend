package com.costumi.backend.pagos.aplicacion;

import java.util.UUID;

/** Datos de la decisión sobre una solicitud de reembolso (aprobar/rechazar con motivo, RF-4.5/6.9). */
public record DecidirReembolsoComando(UUID empresaId, UUID solicitudId, boolean aprobar, UUID actorUsuarioId,
		String actorRol, String motivo) {
}
