package com.costumi.backend.rentas.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * Renta de una prenda (RF-3). Pertenece a una Empresa (tenant) y a una Sucursal; se hace para un
 * Cliente, con fechas de retiro/devolución, importe (precio × periodo, RF-3.3) y depósito.
 * Encapsula la máquina de estados de RF-3.5.
 */
public class Renta {

	private final UUID id;
	private final UUID empresaId;
	private final UUID sucursalId;
	private final UUID clienteId;
	private final UUID prendaId;
	private final LocalDate fechaRetiro;
	private final LocalDate fechaDevolucion;
	private final BigDecimal precioPorDia;
	private final BigDecimal deposito;
	private final BigDecimal importe;
	private EstadoRenta estado;

	private Renta(UUID id, UUID empresaId, UUID sucursalId, UUID clienteId, UUID prendaId, LocalDate fechaRetiro,
			LocalDate fechaDevolucion, BigDecimal precioPorDia, BigDecimal deposito, BigDecimal importe,
			EstadoRenta estado) {
		this.id = Objects.requireNonNull(id, "id");
		this.empresaId = Objects.requireNonNull(empresaId, "empresaId");
		this.sucursalId = Objects.requireNonNull(sucursalId, "sucursalId");
		this.clienteId = Objects.requireNonNull(clienteId, "clienteId");
		this.prendaId = Objects.requireNonNull(prendaId, "prendaId");
		this.fechaRetiro = Objects.requireNonNull(fechaRetiro, "fechaRetiro");
		this.fechaDevolucion = Objects.requireNonNull(fechaDevolucion, "fechaDevolucion");
		this.precioPorDia = precioPorDia;
		this.deposito = deposito;
		this.importe = importe;
		this.estado = Objects.requireNonNull(estado, "estado");
	}

	public static Renta crear(UUID empresaId, UUID sucursalId, UUID clienteId, UUID prendaId, LocalDate fechaRetiro,
			LocalDate fechaDevolucion, BigDecimal precioPorDia, BigDecimal deposito) {
		if (fechaDevolucion.isBefore(fechaRetiro)) {
			throw new IllegalArgumentException("La fecha de devolución no puede ser anterior a la de retiro");
		}
		if (precioPorDia == null || precioPorDia.signum() <= 0) {
			throw new IllegalArgumentException("El precio por día debe ser mayor a 0");
		}
		BigDecimal dep = (deposito == null) ? BigDecimal.ZERO : deposito;
		if (dep.signum() < 0) {
			throw new IllegalArgumentException("El depósito no puede ser negativo");
		}
		BigDecimal importe = precioPorDia.multiply(BigDecimal.valueOf(dias(fechaRetiro, fechaDevolucion)));
		return new Renta(UUID.randomUUID(), empresaId, sucursalId, clienteId, prendaId, fechaRetiro, fechaDevolucion,
				precioPorDia, dep, importe, EstadoRenta.RESERVADA);
	}

	public static Renta rehidratar(UUID id, UUID empresaId, UUID sucursalId, UUID clienteId, UUID prendaId,
			LocalDate fechaRetiro, LocalDate fechaDevolucion, BigDecimal precioPorDia, BigDecimal deposito,
			BigDecimal importe, EstadoRenta estado) {
		return new Renta(id, empresaId, sucursalId, clienteId, prendaId, fechaRetiro, fechaDevolucion, precioPorDia,
				deposito, importe, estado);
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

	public UUID prendaId() {
		return prendaId;
	}

	public LocalDate fechaRetiro() {
		return fechaRetiro;
	}

	public LocalDate fechaDevolucion() {
		return fechaDevolucion;
	}

	public BigDecimal precioPorDia() {
		return precioPorDia;
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
