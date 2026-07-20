package com.costumi.backend.rentas.adaptadores.entrada;

import com.costumi.backend.compartido.CodigoDeRetiro;
import com.costumi.backend.inventario.ConsultaDeInventario.ResumenDePrenda;
import com.costumi.backend.rentas.dominio.Renta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO de salida de la Renta. Expone el detalle multi-artículo en {@code lineas} (cada una con nombre y
 * foto de la prenda), el {@code codigoRetiro} que el cliente muestra en la tienda para retirar, y por
 * compatibilidad el artículo principal en {@code prendaId}/{@code precioPorDia}.
 */
public record RentaResponse(UUID id, String codigoRetiro, UUID sucursalId, UUID clienteId, String clienteNombre,
		UUID empleadoId, List<LineaRentaResponse> lineas, UUID prendaId, LocalDate fechaRetiro,
		LocalDate fechaDevolucion, BigDecimal precioPorDia, BigDecimal deposito, BigDecimal importe, String estado) {

	/** Sin resumen de prendas ni nombre: las líneas van sin nombre/foto (para PDF u otros usos internos). */
	static RentaResponse desde(Renta r) {
		return desde(r, Map.of(), null);
	}

	/**
	 * Enriquecida: cada línea toma nombre y foto de {@code resumenes} (por prendaId) y {@code clienteNombre}
	 * es el nombre de la ficha del cliente, para que el listado lo muestre sin abrir el detalle.
	 */
	static RentaResponse desde(Renta r, Map<UUID, ResumenDePrenda> resumenes, String clienteNombre) {
		List<LineaRentaResponse> lineas = r.lineas().stream()
				.map(l -> LineaRentaResponse.desde(l, resumenes.get(l.prendaId())))
				.toList();
		return new RentaResponse(r.id(), CodigoDeRetiro.de("R", r.id()), r.sucursalId(), r.clienteId(), clienteNombre,
				r.empleadoId(), lineas, r.prendaId(), r.fechaRetiro(), r.fechaDevolucion(), r.precioPorDia(),
				r.deposito(), r.importe(), r.estado().name());
	}
}
