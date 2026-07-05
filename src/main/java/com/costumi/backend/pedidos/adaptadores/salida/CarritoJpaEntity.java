package com.costumi.backend.pedidos.adaptadores.salida;

import com.costumi.backend.compartido.FiltroTenant;
import org.hibernate.annotations.Filter;

import com.costumi.backend.pedidos.dominio.EstadoCarrito;
import com.costumi.backend.pedidos.dominio.TipoPedido;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Mapeo JPA de la cabecera del Carrito. Lleva {@code empresa_id} (tenant). */
@Entity
@Table(name = "carrito")
@Filter(name = FiltroTenant.NOMBRE)
class CarritoJpaEntity {

	@Id
	private UUID id;

	@Column(name = "empresa_id", nullable = false)
	private UUID empresaId;

	@Column(name = "sucursal_id", nullable = false)
	private UUID sucursalId;

	@Column(name = "cliente_id", nullable = false)
	private UUID clienteId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private TipoPedido tipo;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 12)
	private EstadoCarrito estado;

	protected CarritoJpaEntity() {
		// requerido por JPA
	}

	CarritoJpaEntity(UUID id, UUID empresaId, UUID sucursalId, UUID clienteId, TipoPedido tipo, EstadoCarrito estado) {
		this.id = id;
		this.empresaId = empresaId;
		this.sucursalId = sucursalId;
		this.clienteId = clienteId;
		this.tipo = tipo;
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

	UUID getClienteId() {
		return clienteId;
	}

	TipoPedido getTipo() {
		return tipo;
	}

	EstadoCarrito getEstado() {
		return estado;
	}
}
