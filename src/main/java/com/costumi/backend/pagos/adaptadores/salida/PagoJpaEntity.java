package com.costumi.backend.pagos.adaptadores.salida;

import com.costumi.backend.compartido.FiltroTenant;
import org.hibernate.annotations.Filter;

import com.costumi.backend.pagos.dominio.MetodoPago;
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

/** Mapeo JPA del Pago. Lleva {@code empresa_id} (tenant). */
@Entity
@Table(name = "pago")
@Filter(name = FiltroTenant.NOMBRE)
class PagoJpaEntity {

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

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 15)
	private MetodoPago metodo;

	@Column(length = 120)
	private String referencia;

	@Column(nullable = false)
	private Instant fecha;

	@Column(name = "clave_idempotencia", length = 120)
	private String claveIdempotencia;

	protected PagoJpaEntity() {
		// requerido por JPA
	}

	PagoJpaEntity(UUID id, UUID empresaId, UUID sucursalId, UUID empleadoId, TipoConcepto tipoConcepto,
			UUID conceptoId, BigDecimal monto, MetodoPago metodo, String referencia, Instant fecha,
			String claveIdempotencia) {
		this.id = id;
		this.empresaId = empresaId;
		this.sucursalId = sucursalId;
		this.empleadoId = empleadoId;
		this.tipoConcepto = tipoConcepto;
		this.conceptoId = conceptoId;
		this.monto = monto;
		this.metodo = metodo;
		this.referencia = referencia;
		this.fecha = fecha;
		this.claveIdempotencia = claveIdempotencia;
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

	MetodoPago getMetodo() {
		return metodo;
	}

	String getReferencia() {
		return referencia;
	}

	Instant getFecha() {
		return fecha;
	}

	String getClaveIdempotencia() {
		return claveIdempotencia;
	}
}
