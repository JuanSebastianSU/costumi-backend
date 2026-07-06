package com.costumi.backend.pagos.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Pago ligado a una renta o venta (RF-6.1). Registra monto, método y (para tarjeta/transferencia)
 * su referencia. Admite una clave de idempotencia para no duplicar cobros (RF-17.6, CLAUDE.md).
 */
public class Pago {

	private final UUID id;
	private final UUID empresaId;
	private final UUID sucursalId;
	private final UUID empleadoId;
	private final TipoConcepto tipoConcepto;
	private final UUID conceptoId;
	private final BigDecimal monto;
	private final TipoPago tipoPago;
	private final MetodoPago metodo;
	private final String referencia;
	private final Instant fecha;
	private final String claveIdempotencia;

	private Pago(UUID id, UUID empresaId, UUID sucursalId, UUID empleadoId, TipoConcepto tipoConcepto, UUID conceptoId,
			BigDecimal monto, TipoPago tipoPago, MetodoPago metodo, String referencia, Instant fecha,
			String claveIdempotencia) {
		this.id = Objects.requireNonNull(id, "id");
		this.empresaId = Objects.requireNonNull(empresaId, "empresaId");
		this.sucursalId = Objects.requireNonNull(sucursalId, "sucursalId");
		this.empleadoId = Objects.requireNonNull(empleadoId, "empleadoId");
		this.tipoConcepto = Objects.requireNonNull(tipoConcepto, "tipoConcepto");
		this.conceptoId = Objects.requireNonNull(conceptoId, "conceptoId");
		this.tipoPago = Objects.requireNonNull(tipoPago, "tipoPago");
		this.metodo = Objects.requireNonNull(metodo, "metodo");
		if (monto == null || monto.signum() <= 0) {
			throw new IllegalArgumentException("El monto del pago debe ser mayor a 0");
		}
		this.monto = monto;
		this.referencia = (referencia == null || referencia.isBlank()) ? null : referencia.trim();
		this.fecha = Objects.requireNonNull(fecha, "fecha");
		this.claveIdempotencia = (claveIdempotencia == null || claveIdempotencia.isBlank()) ? null
				: claveIdempotencia.trim();
	}

	public static Pago registrar(UUID empresaId, UUID sucursalId, UUID empleadoId, TipoConcepto tipoConcepto,
			UUID conceptoId, BigDecimal monto, TipoPago tipoPago, MetodoPago metodo, String referencia,
			String claveIdempotencia) {
		return new Pago(UUID.randomUUID(), empresaId, sucursalId, empleadoId, tipoConcepto, conceptoId, monto,
				tipoPago == null ? TipoPago.COBRO : tipoPago, metodo, referencia, Instant.now(), claveIdempotencia);
	}

	public static Pago rehidratar(UUID id, UUID empresaId, UUID sucursalId, UUID empleadoId, TipoConcepto tipoConcepto,
			UUID conceptoId, BigDecimal monto, TipoPago tipoPago, MetodoPago metodo, String referencia, Instant fecha,
			String claveIdempotencia) {
		return new Pago(id, empresaId, sucursalId, empleadoId, tipoConcepto, conceptoId, monto, tipoPago, metodo,
				referencia, fecha, claveIdempotencia);
	}

	/** Monto neto: el cobro suma, el reembolso resta (para el saldo de la operación, RF-6.9). */
	public BigDecimal montoNeto() {
		return tipoPago == TipoPago.REEMBOLSO ? monto.negate() : monto;
	}

	public TipoPago tipoPago() {
		return tipoPago;
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

	public TipoConcepto tipoConcepto() {
		return tipoConcepto;
	}

	public UUID conceptoId() {
		return conceptoId;
	}

	public BigDecimal monto() {
		return monto;
	}

	public MetodoPago metodo() {
		return metodo;
	}

	public String referencia() {
		return referencia;
	}

	public Instant fecha() {
		return fecha;
	}

	public String claveIdempotencia() {
		return claveIdempotencia;
	}
}
