package com.costumi.backend.ventas.dominio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Venta / transacción de POS (RF-4). Va a nombre del empleado logueado (RF-4.2); el cliente es
 * opcional. Lleva sus líneas, un descuento y el total. Agregado de dominio.
 */
public class Venta {

	private final UUID id;
	private final UUID empresaId;
	private final UUID sucursalId;
	private final UUID empleadoId;
	private final UUID clienteId;
	private final BigDecimal descuento;
	private final BigDecimal total;
	private EstadoVenta estado;
	private final List<LineaDeVenta> lineas;
	private final String claveIdempotencia;

	private Venta(UUID id, UUID empresaId, UUID sucursalId, UUID empleadoId, UUID clienteId, BigDecimal descuento,
			BigDecimal total, EstadoVenta estado, List<LineaDeVenta> lineas, String claveIdempotencia) {
		this.id = Objects.requireNonNull(id, "id");
		this.empresaId = Objects.requireNonNull(empresaId, "empresaId");
		this.sucursalId = Objects.requireNonNull(sucursalId, "sucursalId");
		this.empleadoId = Objects.requireNonNull(empleadoId, "empleadoId");
		this.clienteId = clienteId;
		this.descuento = descuento;
		this.total = total;
		this.estado = Objects.requireNonNull(estado, "estado");
		this.lineas = new ArrayList<>(lineas);
		this.claveIdempotencia = claveIdempotencia;
	}

	public static Venta crear(UUID empresaId, UUID sucursalId, UUID empleadoId, UUID clienteId, BigDecimal descuento,
			List<LineaDeVenta> lineas, String claveIdempotencia) {
		if (lineas == null || lineas.isEmpty()) {
			throw new IllegalArgumentException("La venta debe tener al menos una línea");
		}
		BigDecimal subtotal = lineas.stream().map(LineaDeVenta::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal dscto = (descuento == null) ? BigDecimal.ZERO : descuento;
		if (dscto.signum() < 0) {
			throw new IllegalArgumentException("El descuento no puede ser negativo");
		}
		if (dscto.compareTo(subtotal) > 0) {
			throw new IllegalArgumentException("El descuento no puede exceder el subtotal");
		}
		return new Venta(UUID.randomUUID(), empresaId, sucursalId, empleadoId, clienteId, dscto,
				subtotal.subtract(dscto), EstadoVenta.CONFIRMADA, List.copyOf(lineas), claveIdempotencia);
	}

	public static Venta rehidratar(UUID id, UUID empresaId, UUID sucursalId, UUID empleadoId, UUID clienteId,
			BigDecimal descuento, BigDecimal total, EstadoVenta estado, List<LineaDeVenta> lineas,
			String claveIdempotencia) {
		return new Venta(id, empresaId, sucursalId, empleadoId, clienteId, descuento, total, estado, lineas,
				claveIdempotencia);
	}

	/**
	 * Devuelve la venta (RF-4.5): pasa de CONFIRMADA a DEVUELTA. Solo una venta confirmada puede
	 * devolverse; el reintegro del dinero y el reingreso de stock los coordina el caso de uso.
	 */
	public void devolver() {
		if (estado != EstadoVenta.CONFIRMADA) {
			throw new VentaNoDevolvible(id);
		}
		this.estado = EstadoVenta.DEVUELTA;
	}

	public List<LineaDeVenta> lineas() {
		return List.copyOf(lineas);
	}

	public UUID id() {
		return id;
	}

	public UUID empresaId() {
		return empresaId;
	}

	public UUID sucursalId() {
		return sucursalId;
	}

	public UUID empleadoId() {
		return empleadoId;
	}

	public UUID clienteId() {
		return clienteId;
	}

	public BigDecimal descuento() {
		return descuento;
	}

	public BigDecimal total() {
		return total;
	}

	public EstadoVenta estado() {
		return estado;
	}

	/** Clave de idempotencia (RF-17.6); null si la venta no vino con una. */
	public String claveIdempotencia() {
		return claveIdempotencia;
	}
}
