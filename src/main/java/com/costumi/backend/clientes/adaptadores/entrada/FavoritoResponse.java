package com.costumi.backend.clientes.adaptadores.entrada;

import com.costumi.backend.clientes.dominio.Favorito;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** DTO de salida de un disfraz favorito ("Mis guardados", C4). */
public record FavoritoResponse(UUID disfrazId, UUID empresaId, String nombre, String fotoUrl, BigDecimal precioRenta,
		BigDecimal precioVenta, Instant guardadoEn) {

	static FavoritoResponse desde(Favorito f) {
		return new FavoritoResponse(f.disfrazId(), f.empresaId(), f.nombre(), f.fotoUrl(), f.precioRenta(),
				f.precioVenta(), f.guardadoEn());
	}
}
