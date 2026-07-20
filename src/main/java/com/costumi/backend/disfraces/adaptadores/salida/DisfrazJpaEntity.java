package com.costumi.backend.disfraces.adaptadores.salida;

import com.costumi.backend.compartido.FiltroTenant;
import org.hibernate.annotations.Filter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/** Mapeo JPA de la cabecera del Disfraz. Lleva {@code empresa_id} (tenant). */
@Entity
@Table(name = "disfraz")
@Filter(name = FiltroTenant.NOMBRE)
class DisfrazJpaEntity {

	@Id
	private UUID id;

	@Column(name = "empresa_id", nullable = false)
	private UUID empresaId;

	@Column(nullable = false, length = 160)
	private String nombre;

	@Column(nullable = false)
	private boolean activo;

	@Column(name = "precio_renta_general", precision = 12, scale = 2)
	private BigDecimal precioRentaGeneral;

	@Column(name = "foto_url", length = 500)
	private String fotoUrl;

	protected DisfrazJpaEntity() {
		// requerido por JPA
	}

	DisfrazJpaEntity(UUID id, UUID empresaId, String nombre, boolean activo, BigDecimal precioRentaGeneral,
			String fotoUrl) {
		this.id = id;
		this.empresaId = empresaId;
		this.nombre = nombre;
		this.activo = activo;
		this.precioRentaGeneral = precioRentaGeneral;
		this.fotoUrl = fotoUrl;
	}

	UUID getId() {
		return id;
	}

	UUID getEmpresaId() {
		return empresaId;
	}

	String getNombre() {
		return nombre;
	}

	boolean isActivo() {
		return activo;
	}

	BigDecimal getPrecioRentaGeneral() {
		return precioRentaGeneral;
	}

	String getFotoUrl() {
		return fotoUrl;
	}
}
