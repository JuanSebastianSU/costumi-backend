package com.costumi.backend.pedidos.dominio;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Carrito / pedido pendiente (RF-16). Segmentación estricta (RF-16.2): identificado por
 * (empresa × sucursal × cliente × tipo); jamás mezcla locales ni tipos. Persistente en el
 * servidor (RF-16.5). Agregado de dominio que contiene sus líneas.
 */
public class Carrito {

	private final UUID id;
	private final UUID empresaId;
	private final UUID sucursalId;
	private final UUID clienteId;
	private final TipoPedido tipo;
	private EstadoCarrito estado;
	private final List<LineaDeCarrito> lineas;

	private Carrito(UUID id, UUID empresaId, UUID sucursalId, UUID clienteId, TipoPedido tipo,
			EstadoCarrito estado, List<LineaDeCarrito> lineas) {
		this.id = Objects.requireNonNull(id, "id");
		this.empresaId = Objects.requireNonNull(empresaId, "empresaId");
		this.sucursalId = Objects.requireNonNull(sucursalId, "sucursalId");
		this.clienteId = Objects.requireNonNull(clienteId, "clienteId");
		this.tipo = Objects.requireNonNull(tipo, "tipo");
		this.estado = Objects.requireNonNull(estado, "estado");
		this.lineas = new ArrayList<>(lineas);
	}

	public static Carrito crear(UUID empresaId, UUID sucursalId, UUID clienteId, TipoPedido tipo) {
		return new Carrito(UUID.randomUUID(), empresaId, sucursalId, clienteId, tipo,
				EstadoCarrito.PENDIENTE, new ArrayList<>());
	}

	public static Carrito rehidratar(UUID id, UUID empresaId, UUID sucursalId, UUID clienteId, TipoPedido tipo,
			EstadoCarrito estado, List<LineaDeCarrito> lineas) {
		return new Carrito(id, empresaId, sucursalId, clienteId, tipo, estado, lineas);
	}

	/** Agrega una prenda de VENTA (sin fechas); si ya está en el carrito, suma a su cantidad. */
	public void agregarItem(UUID prendaId, int cantidad) {
		agregarItem(prendaId, cantidad, null, null);
	}

	/**
	 * Agrega una prenda con su periodo (RF-18.6). En un carrito de RENTA las fechas son obligatorias; en
	 * uno de VENTA deben ser nulas. Si ya existe una línea con la misma prenda <b>y</b> el mismo periodo,
	 * suma a su cantidad; distinto periodo = otra línea (se rentará en otra renta al hacer checkout).
	 */
	public void agregarItem(UUID prendaId, int cantidad, java.time.LocalDate fechaRetiro,
			java.time.LocalDate fechaDevolucion) {
		if (tipo == TipoPedido.RENTA && (fechaRetiro == null || fechaDevolucion == null)) {
			throw new IllegalArgumentException("Un artículo de renta requiere fechas de retiro y devolución");
		}
		if (tipo == TipoPedido.VENTA && (fechaRetiro != null || fechaDevolucion != null)) {
			throw new IllegalArgumentException("Un artículo de venta no lleva fechas");
		}
		for (LineaDeCarrito linea : lineas) {
			if (linea.mismaClave(prendaId, fechaRetiro, fechaDevolucion)) {
				linea.incrementar(cantidad);
				return;
			}
		}
		lineas.add(LineaDeCarrito.de(prendaId, cantidad, fechaRetiro, fechaDevolucion));
	}

	/** Confirma el carrito al hacer checkout (RF-16): pasa a CONFIRMADO. Debe estar pendiente y no vacío. */
	public void confirmar() {
		if (estado != EstadoCarrito.PENDIENTE) {
			throw new IllegalStateException("El carrito ya no está pendiente");
		}
		if (lineas.isEmpty()) {
			throw new IllegalArgumentException("El carrito está vacío");
		}
		this.estado = EstadoCarrito.CONFIRMADO;
	}

	public List<LineaDeCarrito> lineas() {
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

	public UUID clienteId() {
		return clienteId;
	}

	public TipoPedido tipo() {
		return tipo;
	}

	public EstadoCarrito estado() {
		return estado;
	}
}
