package com.costumi.backend.pagos.adaptadores.salida;

import com.costumi.backend.pagos.dominio.EstadoIntento;
import com.costumi.backend.pagos.dominio.TipoConcepto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Mapeo JPA del intento de pago en línea. Nunca se expone por la API. */
@Entity
@Table(name = "intento_de_pago")
class IntentoDePagoJpaEntity {

	@Id
	private UUID id;

	@Column(name = "empresa_id", nullable = false)
	private UUID empresaId;

	@Column(name = "sucursal_id", nullable = false)
	private UUID sucursalId;

	@Column(name = "empleado_id", nullable = false)
	private UUID empleadoId;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_concepto", nullable = false, length = 10)
	private TipoConcepto tipoConcepto;

	@Column(name = "concepto_id", nullable = false)
	private UUID conceptoId;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal monto;

	@Column(nullable = false, length = 8)
	private String moneda;

	@Column(name = "referencia_externa", length = 200)
	private String referenciaExterna;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 12)
	private EstadoIntento estado;

	@Column(nullable = false)
	private Instant fecha;

	protected IntentoDePagoJpaEntity() {
		// requerido por JPA
	}

	IntentoDePagoJpaEntity(UUID id, UUID empresaId, UUID sucursalId, UUID empleadoId, TipoConcepto tipoConcepto,
			UUID conceptoId, BigDecimal monto, String moneda, String referenciaExterna, EstadoIntento estado,
			Instant fecha) {
		this.id = id;
		this.empresaId = empresaId;
		this.sucursalId = sucursalId;
		this.empleadoId = empleadoId;
		this.tipoConcepto = tipoConcepto;
		this.conceptoId = conceptoId;
		this.monto = monto;
		this.moneda = moneda;
		this.referenciaExterna = referenciaExterna;
		this.estado = estado;
		this.fecha = fecha;
	}

	UUID getId() {
		return id;
	}

	UUID getEmpresaId() {
		return empresaId;
	}

	UUID getSucursalId() {
		return sucursalId;
	}

	UUID getEmpleadoId() {
		return empleadoId;
	}

	TipoConcepto getTipoConcepto() {
		return tipoConcepto;
	}

	UUID getConceptoId() {
		return conceptoId;
	}

	BigDecimal getMonto() {
		return monto;
	}

	String getMoneda() {
		return moneda;
	}

	String getReferenciaExterna() {
		return referenciaExterna;
	}

	EstadoIntento getEstado() {
		return estado;
	}

	Instant getFecha() {
		return fecha;
	}
}
