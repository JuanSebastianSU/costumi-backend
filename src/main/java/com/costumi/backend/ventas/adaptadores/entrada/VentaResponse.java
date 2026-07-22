package com.costumi.backend.ventas.adaptadores.entrada;

import com.costumi.backend.compartido.CodigoDeRetiro;
import com.costumi.backend.inventario.ConsultaDeInventario.ResumenDePrenda;
import com.costumi.backend.ventas.dominio.LineaDeVenta;
import com.costumi.backend.ventas.dominio.Venta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** DTO de salida de la Venta con sus líneas (cada una con nombre y foto) y el {@code codigoRetiro} para retirar. {@code montoReembolsado} es el total ya devuelto (RF-4.5). */
public record VentaResponse(UUID id, String codigoRetiro, UUID sucursalId, UUID empleadoId, UUID clienteId,
		String clienteNombre, BigDecimal descuento, BigDecimal total, String estado, BigDecimal montoReembolsado,
		List<LineaResponse> lineas) {

	/**
	 * Una línea de la venta. Si salió de armar un disfraz, {@code disfrazId}/{@code disfrazNombre} dicen
	 * cuál y {@code disfrazGrupo} identifica esa instancia concreta (dos veces el mismo disfraz con piezas
	 * distintas son dos grupos): así el cliente ve QUÉ compró y no un montón de piezas sueltas.
	 */
	public record LineaResponse(UUID prendaId, String nombre, String fotoUrl, int cantidad, int cantidadDevuelta,
			BigDecimal precioUnitario, BigDecimal subtotal, UUID disfrazId, String disfrazNombre, UUID disfrazGrupo,
			Integer disfrazCantidad) {
	}

	/** Sin resumen de prendas ni nombre: líneas sin nombre/foto (usos internos). */
	static VentaResponse desde(Venta v) {
		return desde(v, Map.of(), null);
	}

	/**
	 * Enriquecida: cada línea toma nombre y foto de {@code resumenes} (por prendaId) y {@code clienteNombre}
	 * es el nombre de la ficha del cliente, para que el listado lo muestre sin abrir el detalle.
	 */
	static VentaResponse desde(Venta v, Map<UUID, ResumenDePrenda> resumenes, String clienteNombre) {
		List<LineaResponse> lineas = v.lineas().stream()
				.map(l -> {
					ResumenDePrenda r = resumenes.get(l.prendaId());
					com.costumi.backend.ventas.dominio.OrigenDisfraz o = l.origenDisfraz();
					return new LineaResponse(l.prendaId(), r == null ? null : r.nombre(), r == null ? null : r.fotoUrl(),
							l.cantidad(), l.cantidadDevuelta(), l.precioUnitario(), l.subtotal(),
							o == null ? null : o.disfrazId(),
							o == null ? null : o.nombre(),
							o == null ? null : o.grupo(),
							o == null ? null : o.cantidad());
				})
				.toList();
		return new VentaResponse(v.id(), CodigoDeRetiro.de("V", v.id()), v.sucursalId(), v.empleadoId(), v.clienteId(),
				clienteNombre, v.descuento(), v.total(), v.estado().name(), montoReembolsado(v), lineas);
	}

	/** Dinero ya reembolsado: la porción del total que corresponde a las unidades devueltas (proporcional). */
	private static BigDecimal montoReembolsado(Venta v) {
		BigDecimal subtotalOriginal = v.lineas().stream().map(LineaDeVenta::subtotal)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		if (subtotalOriginal.signum() == 0) {
			return BigDecimal.ZERO;
		}
		BigDecimal devueltoSubtotal = v.lineas().stream().map(LineaDeVenta::subtotalDevuelto)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		return v.total().multiply(devueltoSubtotal).divide(subtotalOriginal, 2, RoundingMode.HALF_UP);
	}
}
