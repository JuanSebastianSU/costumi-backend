package com.costumi.backend.ventas.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Venta / transacción de POS (RF-4). Va a nombre del empleado logueado (RF-4.2); el cliente es
 * opcional. Lleva sus líneas, un descuento, el total y la fecha en que se registró (para la ventana
 * de reembolso, RF-4.5). Agregado de dominio.
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
	private final Instant creadaEn;

	private Venta(UUID id, UUID empresaId, UUID sucursalId, UUID empleadoId, UUID clienteId, BigDecimal descuento,
			BigDecimal total, EstadoVenta estado, List<LineaDeVenta> lineas, String claveIdempotencia,
			Instant creadaEn) {
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
		this.creadaEn = Objects.requireNonNull(creadaEn, "creadaEn");
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
				subtotal.subtract(dscto), EstadoVenta.CONFIRMADA, List.copyOf(lineas), claveIdempotencia, Instant.now());
	}

	public static Venta rehidratar(UUID id, UUID empresaId, UUID sucursalId, UUID empleadoId, UUID clienteId,
			BigDecimal descuento, BigDecimal total, EstadoVenta estado, List<LineaDeVenta> lineas,
			String claveIdempotencia, Instant creadaEn) {
		return new Venta(id, empresaId, sucursalId, empleadoId, clienteId, descuento, total, estado, lineas,
				claveIdempotencia, creadaEn);
	}

	/**
	 * Devuelve unidades de la venta (RF-4.5). {@code cantidades} = unidades a devolver por prenda;
	 * {@code null}/vacío = <b>todo lo pendiente</b> (devolución total). Solo se puede devolver lo que
	 * aún no se devolvió; si no queda nada pendiente, es {@link VentaNoDevolvible}. Actualiza el estado
	 * (PARCIALMENTE_DEVUELTA o DEVUELTA) y devuelve las unidades efectivamente devueltas por prenda,
	 * para que el caso de uso reingrese el stock y coordine el reintegro del dinero (REEMBOLSO).
	 */
	public Map<UUID, Integer> devolver(Map<UUID, Integer> cantidades) {
		if (estado == EstadoVenta.DEVUELTA) {
			throw new VentaNoDevolvible(id);
		}
		Map<UUID, Integer> efectivas = (cantidades == null || cantidades.isEmpty()) ? pendientePorPrenda()
				: new LinkedHashMap<>(cantidades);
		if (efectivas.isEmpty()) {
			throw new VentaNoDevolvible(id);
		}
		for (Map.Entry<UUID, Integer> entrada : efectivas.entrySet()) {
			LineaDeVenta linea = lineaDe(entrada.getKey());
			if (linea == null) {
				throw new IllegalArgumentException("La prenda a devolver no pertenece a la venta");
			}
			linea.devolver(entrada.getValue());
		}
		this.estado = lineas.stream().allMatch(LineaDeVenta::estaTotalmenteDevuelta)
				? EstadoVenta.DEVUELTA : EstadoVenta.PARCIALMENTE_DEVUELTA;
		return efectivas;
	}

	/** Unidades aún no devueltas por prenda (solo las que tienen pendiente > 0). */
	public Map<UUID, Integer> pendientePorPrenda() {
		Map<UUID, Integer> pendientes = new LinkedHashMap<>();
		for (LineaDeVenta linea : lineas) {
			if (linea.pendiente() > 0) {
				pendientes.merge(linea.prendaId(), linea.pendiente(), Integer::sum);
			}
		}
		return pendientes;
	}

	private LineaDeVenta lineaDe(UUID prendaId) {
		return lineas.stream().filter(l -> l.prendaId().equals(prendaId)).findFirst().orElse(null);
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

	/** Momento en que se registró la venta; base para la ventana de reembolso (RF-4.5). */
	public Instant creadaEn() {
		return creadaEn;
	}
}
