package com.costumi.backend.pagos.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Solicitud de reembolso de una venta o renta (RF-4.5/6.9). Modela el proceso de dos pasos: el cliente
 * (registrado por el personal) la solicita con un motivo, y la sucursal la <b>aprueba o rechaza dejando un
 * motivo</b>. La aprobación es terminal (ya movió dinero); una rechazada la puede revertir un rol superior
 * (escalamiento por pirámide, que valida el caso de uso). El dinero solo se mueve al aprobar y con el ítem ya
 * devuelto (lo exige el caso de uso), para que el reembolso siga a la mercancía y no al revés.
 */
public class SolicitudDeReembolso {

	private final UUID id;
	private final UUID empresaId;
	private final TipoConcepto tipoConcepto;
	private final UUID conceptoId;
	private final UUID solicitanteClienteId; // cliente al que se le reembolsaría; puede ser null
	private final BigDecimal monto;
	private final String motivoSolicitud;
	private EstadoSolicitudReembolso estado;
	private String motivoDecision;
	private UUID decididoPorUsuarioId;
	private String rolDecision; // rol del último que decidió (para el override por pirámide)
	private final Instant creadaEn;
	private Instant decididaEn;

	private SolicitudDeReembolso(UUID id, UUID empresaId, TipoConcepto tipoConcepto, UUID conceptoId,
			UUID solicitanteClienteId, BigDecimal monto, String motivoSolicitud, EstadoSolicitudReembolso estado,
			String motivoDecision, UUID decididoPorUsuarioId, String rolDecision, Instant creadaEn,
			Instant decididaEn) {
		this.id = Objects.requireNonNull(id, "id");
		this.empresaId = Objects.requireNonNull(empresaId, "empresaId");
		this.tipoConcepto = Objects.requireNonNull(tipoConcepto, "tipoConcepto");
		this.conceptoId = Objects.requireNonNull(conceptoId, "conceptoId");
		this.solicitanteClienteId = solicitanteClienteId;
		if (monto == null || monto.signum() <= 0) {
			throw new IllegalArgumentException("El monto del reembolso debe ser mayor a 0");
		}
		this.monto = monto;
		this.motivoSolicitud = exigirMotivo(motivoSolicitud, "El motivo de la solicitud es obligatorio");
		this.estado = Objects.requireNonNull(estado, "estado");
		this.motivoDecision = motivoDecision;
		this.decididoPorUsuarioId = decididoPorUsuarioId;
		this.rolDecision = rolDecision;
		this.creadaEn = Objects.requireNonNull(creadaEn, "creadaEn");
		this.decididaEn = decididaEn;
	}

	public static SolicitudDeReembolso crear(UUID empresaId, TipoConcepto tipoConcepto, UUID conceptoId,
			UUID solicitanteClienteId, BigDecimal monto, String motivoSolicitud) {
		return new SolicitudDeReembolso(UUID.randomUUID(), empresaId, tipoConcepto, conceptoId, solicitanteClienteId,
				monto, motivoSolicitud, EstadoSolicitudReembolso.PENDIENTE, null, null, null, Instant.now(), null);
	}

	public static SolicitudDeReembolso rehidratar(UUID id, UUID empresaId, TipoConcepto tipoConcepto, UUID conceptoId,
			UUID solicitanteClienteId, BigDecimal monto, String motivoSolicitud, EstadoSolicitudReembolso estado,
			String motivoDecision, UUID decididoPorUsuarioId, String rolDecision, Instant creadaEn, Instant decididaEn) {
		return new SolicitudDeReembolso(id, empresaId, tipoConcepto, conceptoId, solicitanteClienteId, monto,
				motivoSolicitud, estado, motivoDecision, decididoPorUsuarioId, rolDecision, creadaEn, decididaEn);
	}

	/**
	 * Registra la decisión (aprobar/rechazar) con su motivo. Una solicitud ya APROBADA es terminal (no se
	 * re-decide). El permiso para revertir una RECHAZADA (escalamiento por pirámide) lo valida el caso de uso.
	 */
	public void decidir(boolean aprobar, UUID actorUsuarioId, String actorRol, String motivo) {
		if (estado == EstadoSolicitudReembolso.APROBADA) {
			throw new IllegalStateException("La solicitud ya fue aprobada: no puede volver a decidirse");
		}
		this.motivoDecision = exigirMotivo(motivo, "El motivo de la decisión es obligatorio");
		this.decididoPorUsuarioId = Objects.requireNonNull(actorUsuarioId, "actorUsuarioId");
		this.rolDecision = actorRol;
		this.estado = aprobar ? EstadoSolicitudReembolso.APROBADA : EstadoSolicitudReembolso.RECHAZADA;
		this.decididaEn = Instant.now();
	}

	public boolean estaPendiente() {
		return estado == EstadoSolicitudReembolso.PENDIENTE;
	}

	public boolean estaRechazada() {
		return estado == EstadoSolicitudReembolso.RECHAZADA;
	}

	private static String exigirMotivo(String motivo, String error) {
		if (motivo == null || motivo.isBlank()) {
			throw new IllegalArgumentException(error);
		}
		return motivo.trim();
	}

	public UUID id() {
		return id;
	}

	public UUID empresaId() {
		return empresaId;
	}

	public TipoConcepto tipoConcepto() {
		return tipoConcepto;
	}

	public UUID conceptoId() {
		return conceptoId;
	}

	public UUID solicitanteClienteId() {
		return solicitanteClienteId;
	}

	public BigDecimal monto() {
		return monto;
	}

	public String motivoSolicitud() {
		return motivoSolicitud;
	}

	public EstadoSolicitudReembolso estado() {
		return estado;
	}

	public String motivoDecision() {
		return motivoDecision;
	}

	public UUID decididoPorUsuarioId() {
		return decididoPorUsuarioId;
	}

	public String rolDecision() {
		return rolDecision;
	}

	public Instant creadaEn() {
		return creadaEn;
	}

	public Instant decididaEn() {
		return decididaEn;
	}
}
