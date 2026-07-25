package com.costumi.backend.clientes.adaptadores.salida;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Mapeo JPA del favorito. NO lleva {@code @Filter} de tenant: es del USUARIO del marketplace (cruza
 * tiendas, como su historial), no de una empresa. La clave lógica (usuario, disfraz) va como constraint único.
 */
@Entity
@Table(name = "favorito_disfraz",
		uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "disfraz_id"}))
class FavoritoJpaEntity {

	@Id
	private UUID id;

	@Column(name = "usuario_id", nullable = false)
	private UUID usuarioId;

	@Column(name = "disfraz_id", nullable = false)
	private UUID disfrazId;

	@Column(name = "empresa_id", nullable = false)
	private UUID empresaId;

	@Column(nullable = false, length = 200)
	private String nombre;

	@Column(name = "foto_url", length = 500)
	private String fotoUrl;

	@Column(name = "precio_renta", precision = 12, scale = 2)
	private BigDecimal precioRenta;

	@Column(name = "precio_venta", precision = 12, scale = 2)
	private BigDecimal precioVenta;

	@Column(name = "guardado_en", nullable = false)
	private Instant guardadoEn;

	protected FavoritoJpaEntity() {
		// requerido por JPA
	}

	FavoritoJpaEntity(UUID id, UUID usuarioId, UUID disfrazId, UUID empresaId, String nombre, String fotoUrl,
			BigDecimal precioRenta, BigDecimal precioVenta, Instant guardadoEn) {
		this.id = id;
		this.usuarioId = usuarioId;
		this.disfrazId = disfrazId;
		this.empresaId = empresaId;
		this.nombre = nombre;
		this.fotoUrl = fotoUrl;
		this.precioRenta = precioRenta;
		this.precioVenta = precioVenta;
		this.guardadoEn = guardadoEn;
	}

	UUID getId() {
		return id;
	}

	UUID getUsuarioId() {
		return usuarioId;
	}

	UUID getDisfrazId() {
		return disfrazId;
	}

	UUID getEmpresaId() {
		return empresaId;
	}

	String getNombre() {
		return nombre;
	}

	String getFotoUrl() {
		return fotoUrl;
	}

	BigDecimal getPrecioRenta() {
		return precioRenta;
	}

	BigDecimal getPrecioVenta() {
		return precioVenta;
	}

	Instant getGuardadoEn() {
		return guardadoEn;
	}
}
