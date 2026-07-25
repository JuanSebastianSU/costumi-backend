package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.identidad.dominio.Membresia;

import java.util.UUID;

/** Estado de una membresía de trabajo tras una acción de desvinculación (Fase B). */
public record MembresiaEstadoResponse(UUID usuarioId, UUID empresaId, String rol, String estado) {

	static MembresiaEstadoResponse desde(Membresia m) {
		return new MembresiaEstadoResponse(m.usuarioId(), m.empresaId(), m.rol().name(), m.estado().name());
	}
}
