package com.costumi.backend.disfraces.adaptadores.salida;

import com.costumi.backend.compartido.FiltroTenant;
import com.costumi.backend.disfraces.dominio.TipoDeDisfraz;
import org.hibernate.annotations.Filter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

	@Column(name = "categoria_id")
	private UUID categoriaId;

	@Column(nullable = false)
	private boolean activo;

	@Column(name = "precio_renta_general", precision = 12, scale = 2)
	private BigDecimal precioRentaGeneral;

	@Column(name = "precio_venta_general", precision = 12, scale = 2)
	private BigDecimal precioVentaGeneral;

	@Column(name = "foto_url", length = 500)
	private String fotoUrl;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private TipoDeDisfraz tipo;

	protected DisfrazJpaEntity() {
		// requerido por JPA
	}

	DisfrazJpaEntity(UUID id, UUID empresaId, String nombre, UUID categoriaId, boolean activo,
			BigDecimal precioRentaGeneral, BigDecimal precioVentaGeneral, String fotoUrl, TipoDeDisfraz tipo) {
		this.id = id;
		this.empresaId = empresaId;
		this.nombre = nombre;
		this.categoriaId = categoriaId;
		this.activo = activo;
		this.precioRentaGeneral = precioRentaGeneral;
		this.precioVentaGeneral = precioVentaGeneral;
		this.fotoUrl = fotoUrl;
		this.tipo = tipo;
	}

	TipoDeDisfraz getTipo() {
		return tipo;
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

	UUID getCategoriaId() {
		return categoriaId;
	}

	boolean isActivo() {
		return activo;
	}

	BigDecimal getPrecioRentaGeneral() {
		return precioRentaGeneral;
	}

	BigDecimal getPrecioVentaGeneral() {
		return precioVentaGeneral;
	}

	String getFotoUrl() {
		return fotoUrl;
	}
}
