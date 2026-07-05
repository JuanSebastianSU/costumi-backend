package com.costumi.backend.ventas.adaptadores.salida;

import com.costumi.backend.compartido.FiltroTenant;
import org.hibernate.annotations.Filter;

import com.costumi.backend.ventas.dominio.EstadoVenta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/** Mapeo JPA de la cabecera de la Venta. Lleva {@code empresa_id} (tenant). */
@Entity
@Table(name = "venta")
@Filter(name = FiltroTenant.NOMBRE)
class VentaJpaEntity {

	@Id
	private UUID id;

	@Column(name = "empresa_id", nullable = false)
	private UUID empresaId;

	@Column(name = "sucursal_id", nullable = false)
	private UUID sucursalId;

	@Column(name = "empleado_id", nullable = false)
	private UUID empleadoId;

	@Column(name = "cliente_id")
	private UUID clienteId;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal descuento;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal total;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 12)
	private EstadoVenta estado;

	protected VentaJpaEntity() {
		// requerido por JPA
	}

	VentaJpaEntity(UUID id, UUID empresaId, UUID sucursalId, UUID empleadoId, UUID clienteId, BigDecimal descuento,
			BigDecimal total, EstadoVenta estado) {
		this.id = id;
		this.empresaId = empresaId;
		this.sucursalId = sucursalId;
		this.empleadoId = empleadoId;
		this.clienteId = clienteId;
		this.descuento = descuento;
		this.total = total;
		this.estado = estado;
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

	UUID getClienteId() {
		return clienteId;
	}

	BigDecimal getDescuento() {
		return descuento;
	}

	BigDecimal getTotal() {
		return total;
	}

	EstadoVenta getEstado() {
		return estado;
	}
}
