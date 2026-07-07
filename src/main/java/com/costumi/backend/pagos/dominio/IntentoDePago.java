package com.costumi.backend.pagos.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Intento de pago en línea (RF-6.11): representa un checkout iniciado en la pasarela. Cuando el webhook
 * confirma el pago, se registra el {@link Pago} correspondiente y el intento pasa a CONFIRMADO.
 */
public class IntentoDePago {

	private final UUID id;
	private final UUID empresaId;
	private final UUID sucursalId;
	private final UUID empleadoId; // staff que inició el intento (se atribuye el Pago al confirmar)
	private final TipoConcepto tipoConcepto;
	private final UUID conceptoId;
	private final BigDecimal monto;
	private final String moneda;
	private final Instant fecha;
	private String referenciaExterna; // id de la preferencia/checkout en la pasarela
	private EstadoIntento estado;

	private IntentoDePago(UUID id, UUID empresaId, UUID sucursalId, UUID empleadoId, TipoConcepto tipoConcepto,
			UUID conceptoId, BigDecimal monto, String moneda, String referenciaExterna, EstadoIntento estado,
			Instant fecha) {
		this.id = Objects.requireNonNull(id, "id");
		this.empresaId = Objects.requireNonNull(empresaId, "empresaId");
		this.sucursalId = Objects.requireNonNull(sucursalId, "sucursalId");
		this.empleadoId = Objects.requireNonNull(empleadoId, "empleadoId");
		this.tipoConcepto = Objects.requireNonNull(tipoConcepto, "tipoConcepto");
		this.conceptoId = Objects.requireNonNull(conceptoId, "conceptoId");
		this.monto = Objects.requireNonNull(monto, "monto");
		this.moneda = (moneda == null || moneda.isBlank()) ? "ARS" : moneda.trim();
		this.referenciaExterna = referenciaExterna;
		this.estado = Objects.requireNonNull(estado, "estado");
		this.fecha = Objects.requireNonNull(fecha, "fecha");
	}

	public static IntentoDePago crear(UUID empresaId, UUID sucursalId, UUID empleadoId, TipoConcepto tipoConcepto,
			UUID conceptoId, BigDecimal monto, String moneda) {
		return new IntentoDePago(UUID.randomUUID(), empresaId, sucursalId, empleadoId, tipoConcepto, conceptoId, monto,
				moneda, null, EstadoIntento.PENDIENTE, Instant.now());
	}

	public static IntentoDePago rehidratar(UUID id, UUID empresaId, UUID sucursalId, UUID empleadoId,
			TipoConcepto tipoConcepto, UUID conceptoId, BigDecimal monto, String moneda, String referenciaExterna,
			EstadoIntento estado, Instant fecha) {
		return new IntentoDePago(id, empresaId, sucursalId, empleadoId, tipoConcepto, conceptoId, monto, moneda,
				referenciaExterna, estado, fecha);
	}

	public UUID empleadoId() {
		return empleadoId;
	}

	public void asignarReferenciaExterna(String referencia) {
		this.referenciaExterna = referencia;
	}

	public void confirmar() {
		this.estado = EstadoIntento.CONFIRMADO;
	}

	public boolean estaConfirmado() {
		return estado == EstadoIntento.CONFIRMADO;
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

	public TipoConcepto tipoConcepto() {
		return tipoConcepto;
	}

	public UUID conceptoId() {
		return conceptoId;
	}

	public BigDecimal monto() {
		return monto;
	}

	public String moneda() {
		return moneda;
	}

	public String referenciaExterna() {
		return referenciaExterna;
	}

	public EstadoIntento estado() {
		return estado;
	}

	public Instant fecha() {
		return fecha;
	}
}
