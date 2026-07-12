package com.costumi.backend.pagos.adaptadores.salida;

import com.costumi.backend.compartido.FiltroTenant;
import com.costumi.backend.pagos.dominio.EstadoSolicitudReembolso;
import com.costumi.backend.pagos.dominio.TipoConcepto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Mapeo JPA de la solicitud de reembolso. Lleva {@code empresa_id} (tenant). */
@Entity
@Table(name = "solicitud_reembolso")
@Filter(name = FiltroTenant.NOMBRE)
class SolicitudDeReembolsoJpaEntity {

	@Id
	private UUID id;

	@Column(name = "empresa_id", nullable = false)
	private UUID empresaId;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_concepto", nullable = false, length = 10)
	private TipoConcepto tipoConcepto;

	@Column(name = "concepto_id", nullable = false)
	private UUID conceptoId;

	@Column(name = "solicitante_cliente_id")
	private UUID solicitanteClienteId;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal monto;

	@Column(name = "motivo_solicitud", nullable = false, length = 500)
	private String motivoSolicitud;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 12)
	private EstadoSolicitudReembolso estado;

	@Column(name = "motivo_decision", length = 500)
	private String motivoDecision;

	@Column(name = "decidido_por_usuario_id")
	private UUID decididoPorUsuarioId;

	@Column(name = "rol_decision", length = 20)
	private String rolDecision;

	@Column(name = "creada_en", nullable = false)
	private Instant creadaEn;

	@Column(name = "decidida_en")
	private Instant decididaEn;

	protected SolicitudDeReembolsoJpaEntity() {
		// requerido por JPA
	}

	SolicitudDeReembolsoJpaEntity(UUID id, UUID empresaId, TipoConcepto tipoConcepto, UUID conceptoId,
			UUID solicitanteClienteId, BigDecimal monto, String motivoSolicitud, EstadoSolicitudReembolso estado,
			String motivoDecision, UUID decididoPorUsuarioId, String rolDecision, Instant creadaEn, Instant decididaEn) {
		this.id = id;
		this.empresaId = empresaId;
		this.tipoConcepto = tipoConcepto;
		this.conceptoId = conceptoId;
		this.solicitanteClienteId = solicitanteClienteId;
		this.monto = monto;
		this.motivoSolicitud = motivoSolicitud;
		this.estado = estado;
		this.motivoDecision = motivoDecision;
		this.decididoPorUsuarioId = decididoPorUsuarioId;
		this.rolDecision = rolDecision;
		this.creadaEn = creadaEn;
		this.decididaEn = decididaEn;
	}

	UUID getId() {
		return id;
	}

	UUID getEmpresaId() {
		return empresaId;
	}

	TipoConcepto getTipoConcepto() {
		return tipoConcepto;
	}

	UUID getConceptoId() {
		return conceptoId;
	}

	UUID getSolicitanteClienteId() {
		return solicitanteClienteId;
	}

	BigDecimal getMonto() {
		return monto;
	}

	String getMotivoSolicitud() {
		return motivoSolicitud;
	}

	EstadoSolicitudReembolso getEstado() {
		return estado;
	}

	String getMotivoDecision() {
		return motivoDecision;
	}

	UUID getDecididoPorUsuarioId() {
		return decididoPorUsuarioId;
	}

	String getRolDecision() {
		return rolDecision;
	}

	Instant getCreadaEn() {
		return creadaEn;
	}

	Instant getDecididaEn() {
		return decididaEn;
	}
}
