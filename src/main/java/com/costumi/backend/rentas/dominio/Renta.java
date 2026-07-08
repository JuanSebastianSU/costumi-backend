package com.costumi.backend.rentas.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Renta de uno o más artículos (RF-3). Pertenece a una Empresa (tenant) y a una Sucursal; se hace
 * para un Cliente, con fechas de retiro/devolución, importe (Σ precio×cantidad × periodo, RF-3.3) y
 * depósito. Lleva una o más {@link RentaLinea} (multi-artículo, RF-3.1/3.6/16.2). Encapsula la
 * máquina de estados de RF-3.5.
 */
public class Renta {

	private final UUID id;
	private final UUID empresaId;
	private final UUID sucursalId;
	private final UUID clienteId;
	private final UUID empleadoId;
	private final List<RentaLinea> lineas;
	private final LocalDate fechaRetiro;
	private LocalDate fechaDevolucion;
	private final BigDecimal deposito;
	private BigDecimal importe;
	private EstadoRenta estado;
	private final String claveIdempotencia;

	private Renta(UUID id, UUID empresaId, UUID sucursalId, UUID clienteId, UUID empleadoId, List<RentaLinea> lineas,
			LocalDate fechaRetiro, LocalDate fechaDevolucion, BigDecimal deposito, BigDecimal importe,
			EstadoRenta estado, String claveIdempotencia) {
		this.id = Objects.requireNonNull(id, "id");
		this.empresaId = Objects.requireNonNull(empresaId, "empresaId");
		this.sucursalId = Objects.requireNonNull(sucursalId, "sucursalId");
		this.clienteId = Objects.requireNonNull(clienteId, "clienteId");
		this.empleadoId = empleadoId; // usuario que la registró (RF-1.4); nulo en datos previos a la columna
		Objects.requireNonNull(lineas, "lineas");
		if (lineas.isEmpty()) {
			throw new IllegalArgumentException("La renta debe tener al menos un artículo");
		}
		this.lineas = List.copyOf(lineas);
		this.fechaRetiro = Objects.requireNonNull(fechaRetiro, "fechaRetiro");
		this.fechaDevolucion = Objects.requireNonNull(fechaDevolucion, "fechaDevolucion");
		this.deposito = deposito;
		this.importe = importe;
		this.estado = Objects.requireNonNull(estado, "estado");
		this.claveIdempotencia = (claveIdempotencia == null || claveIdempotencia.isBlank()) ? null
				: claveIdempotencia.trim();
	}

	/** Conveniencia: renta de un solo artículo (cantidad 1). */
	public static Renta crear(UUID empresaId, UUID sucursalId, UUID clienteId, UUID prendaId, LocalDate fechaRetiro,
			LocalDate fechaDevolucion, BigDecimal precioPorDia, BigDecimal deposito) {
		return crear(empresaId, sucursalId, clienteId, prendaId, fechaRetiro, fechaDevolucion, precioPorDia, deposito,
				null);
	}

	/** Conveniencia: renta de un solo artículo (cantidad 1) con clave de idempotencia. */
	public static Renta crear(UUID empresaId, UUID sucursalId, UUID clienteId, UUID prendaId, LocalDate fechaRetiro,
			LocalDate fechaDevolucion, BigDecimal precioPorDia, BigDecimal deposito, String claveIdempotencia) {
		if (precioPorDia == null || precioPorDia.signum() <= 0) {
			throw new IllegalArgumentException("El precio por día debe ser mayor a 0");
		}
		return crear(empresaId, sucursalId, clienteId, List.of(RentaLinea.de(prendaId, 1, precioPorDia)), fechaRetiro,
				fechaDevolucion, deposito, claveIdempotencia, null);
	}

	/** Crea una renta multi-artículo: el importe es Σ (precio×cantidad) de cada línea, por el periodo. */
	public static Renta crear(UUID empresaId, UUID sucursalId, UUID clienteId, List<RentaLinea> lineas,
			LocalDate fechaRetiro, LocalDate fechaDevolucion, BigDecimal deposito, String claveIdempotencia,
			UUID empleadoId) {
		if (fechaDevolucion.isBefore(fechaRetiro)) {
			throw new IllegalArgumentException("La fecha de devolución no puede ser anterior a la de retiro");
		}
		if (lineas == null || lineas.isEmpty()) {
			throw new IllegalArgumentException("La renta debe tener al menos un artículo");
		}
		BigDecimal dep = (deposito == null) ? BigDecimal.ZERO : deposito;
		if (dep.signum() < 0) {
			throw new IllegalArgumentException("El depósito no puede ser negativo");
		}
		BigDecimal importe = importeDe(lineas, fechaRetiro, fechaDevolucion);
		return new Renta(UUID.randomUUID(), empresaId, sucursalId, clienteId, empleadoId, lineas, fechaRetiro,
				fechaDevolucion, dep, importe, EstadoRenta.RESERVADA, claveIdempotencia);
	}

	public static Renta rehidratar(UUID id, UUID empresaId, UUID sucursalId, UUID clienteId, UUID empleadoId,
			List<RentaLinea> lineas, LocalDate fechaRetiro, LocalDate fechaDevolucion, BigDecimal deposito,
			BigDecimal importe, EstadoRenta estado, String claveIdempotencia) {
		return new Renta(id, empresaId, sucursalId, clienteId, empleadoId, lineas, fechaRetiro, fechaDevolucion,
				deposito, importe, estado, claveIdempotencia);
	}

	private static BigDecimal importeDe(List<RentaLinea> lineas, LocalDate retiro, LocalDate devolucion) {
		BigDecimal subtotalPorDia = lineas.stream()
				.map(RentaLinea::subtotalPorDia)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		return subtotalPorDia.multiply(BigDecimal.valueOf(dias(retiro, devolucion)));
	}

	public String claveIdempotencia() {
		return claveIdempotencia;
	}

	/** Días del periodo, mínimo 1. */
	private static long dias(LocalDate retiro, LocalDate devolucion) {
		return Math.max(1, ChronoUnit.DAYS.between(retiro, devolucion));
	}

	public void entregar() {
		transicionarA(EstadoRenta.ACTIVA);
	}

	public void devolver() {
		transicionarA(EstadoRenta.DEVUELTA);
	}

	public void cerrar() {
		transicionarA(EstadoRenta.CERRADA);
	}

	public void cancelar() {
		transicionarA(EstadoRenta.CANCELADA);
	}

	/**
	 * Extiende/renueva la renta a una nueva fecha de devolución posterior (RF-3.6). Solo si está
	 * RESERVADA o ACTIVA; recalcula el importe = Σ precios × nuevo periodo.
	 */
	public void extender(LocalDate nuevaFechaDevolucion) {
		if (estado != EstadoRenta.RESERVADA && estado != EstadoRenta.ACTIVA) {
			throw new IllegalArgumentException("Solo se puede extender una renta RESERVADA o ACTIVA");
		}
		if (!nuevaFechaDevolucion.isAfter(fechaDevolucion)) {
			throw new IllegalArgumentException("La nueva fecha de devolución debe ser posterior a la actual");
		}
		this.fechaDevolucion = nuevaFechaDevolucion;
		this.importe = importeDe(lineas, fechaRetiro, nuevaFechaDevolucion);
	}

	/** Vencida (RF-3.5): activa y ya pasó la fecha de devolución. */
	public boolean estaVencida(LocalDate hoy) {
		return estado == EstadoRenta.ACTIVA && hoy.isAfter(fechaDevolucion);
	}

	private void transicionarA(EstadoRenta destino) {
		if (!estado.puedeTransicionarA(destino)) {
			throw new TransicionDeRentaInvalida(estado, destino);
		}
		this.estado = destino;
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

	/** Usuario (empleado o cliente) que registró la renta (RF-1.4); puede ser nulo en datos previos. */
	public UUID empleadoId() {
		return empleadoId;
	}

	public List<RentaLinea> lineas() {
		return lineas;
	}

	/** Artículo principal (primera línea): conveniencia para vistas/persistencia de una sola prenda. */
	public UUID prendaId() {
		return lineas.get(0).prendaId();
	}

	/** Precio por día del artículo principal (primera línea). */
	public BigDecimal precioPorDia() {
		return lineas.get(0).precioPorDia();
	}

	public LocalDate fechaRetiro() {
		return fechaRetiro;
	}

	public LocalDate fechaDevolucion() {
		return fechaDevolucion;
	}

	public BigDecimal deposito() {
		return deposito;
	}

	public BigDecimal importe() {
		return importe;
	}

	public EstadoRenta estado() {
		return estado;
	}
}
