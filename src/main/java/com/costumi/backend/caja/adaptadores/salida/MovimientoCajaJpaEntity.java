package com.costumi.backend.caja.adaptadores.salida;

import com.costumi.backend.caja.dominio.MetodoDePago;
import com.costumi.backend.caja.dominio.TipoMovimiento;
import com.costumi.backend.compartido.FiltroTenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.util.UUID;

/** Mapeo JPA de un movimiento de caja (hijo del turno). Lleva {@code empresa_id} (tenant). */
@Entity
@Table(name = "movimiento_caja")
@Filter(name = FiltroTenant.NOMBRE)
class MovimientoCajaJpaEntity {

	@Id
	private UUID id;

	@Column(name = "turno_id", nullable = false)
	private UUID turnoId;

	@Column(name = "empresa_id", nullable = false)
	private UUID empresaId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 8)
	private TipoMovimiento tipo;

	@Column(nullable = false, length = 200)
	private String concepto;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal monto;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 14)
	private MetodoDePago metodo;

	@Column(nullable = false)
	private int orden;

	protected MovimientoCajaJpaEntity() {
		// requerido por JPA
	}

	MovimientoCajaJpaEntity(UUID id, UUID turnoId, UUID empresaId, TipoMovimiento tipo, String concepto,
			BigDecimal monto, MetodoDePago metodo, int orden) {
		this.id = id;
		this.turnoId = turnoId;
		this.empresaId = empresaId;
		this.tipo = tipo;
		this.concepto = concepto;
		this.monto = monto;
		this.metodo = metodo;
		this.orden = orden;
	}

	TipoMovimiento getTipo() {
		return tipo;
	}

	String getConcepto() {
		return concepto;
	}

	BigDecimal getMonto() {
		return monto;
	}

	MetodoDePago getMetodo() {
		return metodo;
	}
}
