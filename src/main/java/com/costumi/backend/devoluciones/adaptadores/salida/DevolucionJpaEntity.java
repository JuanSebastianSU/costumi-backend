package com.costumi.backend.devoluciones.adaptadores.salida;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/** Mapeo JPA de la cabecera de la Devolución. Lleva {@code empresa_id} (tenant). */
@Entity
@Table(name = "devolucion")
class DevolucionJpaEntity {

	@Id
	private UUID id;

	@Column(name = "empresa_id", nullable = false)
	private UUID empresaId;

	@Column(name = "renta_id", nullable = false)
	private UUID rentaId;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal deposito;

	@Column(name = "cargo_por_danos", nullable = false, precision = 12, scale = 2)
	private BigDecimal cargoPorDanos;

	@Column(name = "cargo_por_retraso", nullable = false, precision = 12, scale = 2)
	private BigDecimal cargoPorRetraso;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal remanente;

	protected DevolucionJpaEntity() {
		// requerido por JPA
	}

	DevolucionJpaEntity(UUID id, UUID empresaId, UUID rentaId, BigDecimal deposito, BigDecimal cargoPorDanos,
			BigDecimal cargoPorRetraso, BigDecimal remanente) {
		this.id = id;
		this.empresaId = empresaId;
		this.rentaId = rentaId;
		this.deposito = deposito;
		this.cargoPorDanos = cargoPorDanos;
		this.cargoPorRetraso = cargoPorRetraso;
		this.remanente = remanente;
	}

	UUID getId() {
		return id;
	}

	UUID getEmpresaId() {
		return empresaId;
	}

	UUID getRentaId() {
		return rentaId;
	}

	BigDecimal getDeposito() {
		return deposito;
	}

	BigDecimal getCargoPorDanos() {
		return cargoPorDanos;
	}

	BigDecimal getCargoPorRetraso() {
		return cargoPorRetraso;
	}

	BigDecimal getRemanente() {
		return remanente;
	}
}
