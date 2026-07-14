package com.costumi.backend.notificaciones.adaptadores.entrada;

import com.costumi.backend.notificaciones.dominio.PlantillaDeNotificacion;

/** DTO de salida de una plantilla de mensaje automático. */
public record PlantillaResponse(String tipo, String texto, boolean activa) {

	static PlantillaResponse desde(PlantillaDeNotificacion p) {
		return new PlantillaResponse(p.tipo().name(), p.texto(), p.activa());
	}
}
