package com.costumi.backend.marketplace.adaptadores.entrada;

import com.costumi.backend.marketplace.dominio.DisfrazDestacado;

import java.math.BigDecimal;
import java.util.UUID;

/** DTO de salida de un disfraz destacado del carrusel del marketplace (C1). */
public record DisfrazDestacadoResponse(UUID disfrazId, UUID empresaId, String empresaNombre, String nombre,
		String fotoUrl, BigDecimal precioRenta, BigDecimal precioVenta) {

	static DisfrazDestacadoResponse desde(DisfrazDestacado d) {
		return new DisfrazDestacadoResponse(d.disfrazId(), d.empresaId(), d.empresaNombre(), d.nombre(), d.fotoUrl(),
				d.precioRenta(), d.precioVenta());
	}
}
