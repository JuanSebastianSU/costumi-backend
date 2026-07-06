package com.costumi.backend.caja.adaptadores.salida;

import com.costumi.backend.caja.dominio.EstadoTurno;
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

/** Mapeo JPA de la cabecera del Turno de caja. Lleva {@code empresa_id} (tenant). */
@Entity
@Table(name = "turno_caja")
@Filter(name = FiltroTenant.NOMBRE)
class TurnoJpaEntity {

	@Id
	private UUID id;

	@Column(name = "empresa_id", nullable = false)
	private UUID empresaId;

	@Column(name = "sucursal_id", nullable = false)
	private UUID sucursalId;

	@Column(name = "empleado_id", nullable = false)
	private UUID empleadoId;

	@Column(name = "fondo_inicial", nullable = false, precision = 12, scale = 2)
	private BigDecimal fondoInicial;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 8)
	private EstadoTurno estado;

	@Column(name = "efectivo_contado", precision = 12, scale = 2)
	private BigDecimal efectivoContado;

	protected TurnoJpaEntity() {
		// requerido por JPA
	}

	TurnoJpaEntity(UUID id, UUID empresaId, UUID sucursalId, UUID empleadoId, BigDecimal fondoInicial,
			EstadoTurno estado, BigDecimal efectivoContado) {
		this.id = id;
		this.empresaId = empresaId;
		this.sucursalId = sucursalId;
		this.empleadoId = empleadoId;
		this.fondoInicial = fondoInicial;
		this.estado = estado;
		this.efectivoContado = efectivoContado;
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

	BigDecimal getFondoInicial() {
		return fondoInicial;
	}

	EstadoTurno getEstado() {
		return estado;
	}

	BigDecimal getEfectivoContado() {
		return efectivoContado;
	}
}
