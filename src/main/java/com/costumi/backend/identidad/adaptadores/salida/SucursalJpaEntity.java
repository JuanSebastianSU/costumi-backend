package com.costumi.backend.identidad.adaptadores.salida;

import com.costumi.backend.compartido.FiltroTenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.util.UUID;

/**
 * Mapeo JPA de la Sucursal. Lleva {@code empresa_id} (tenant). Aquí se <b>define</b> el filtro de
 * aislamiento multi-tenant (§5.4) que el resto de entidades solo aplican con {@code @Filter}.
 */
@Entity
@Table(name = "sucursal")
@FilterDef(name = FiltroTenant.NOMBRE, parameters = @ParamDef(name = FiltroTenant.PARAM_EMPRESA, type = UUID.class),
		defaultCondition = FiltroTenant.CONDICION)
@Filter(name = FiltroTenant.NOMBRE)
class SucursalJpaEntity {

	@Id
	private UUID id;

	@Column(name = "empresa_id", nullable = false)
	private UUID empresaId;

	@Column(nullable = false, length = 200)
	private String nombre;

	@Column(length = 300)
	private String direccion;

	@Column(name = "ubicacion_maps", length = 500)
	private String ubicacionMaps;

	@Column(nullable = false)
	private boolean archivada;

	@Column(length = 1000)
	private String descripcion;

	private Double latitud;

	private Double longitud;

	@Column(name = "foto_url", length = 500)
	private String fotoUrl;

	protected SucursalJpaEntity() {
		// requerido por JPA
	}

	SucursalJpaEntity(UUID id, UUID empresaId, String nombre, String direccion, String ubicacionMaps,
			boolean archivada, String descripcion, Double latitud, Double longitud, String fotoUrl) {
		this.id = id;
		this.empresaId = empresaId;
		this.nombre = nombre;
		this.direccion = direccion;
		this.ubicacionMaps = ubicacionMaps;
		this.archivada = archivada;
		this.descripcion = descripcion;
		this.latitud = latitud;
		this.longitud = longitud;
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

	String getDireccion() {
		return direccion;
	}

	String getUbicacionMaps() {
		return ubicacionMaps;
	}

	boolean isArchivada() {
		return archivada;
	}

	String getDescripcion() {
		return descripcion;
	}

	Double getLatitud() {
		return latitud;
	}

	Double getLongitud() {
		return longitud;
	}

	String getFotoUrl() {
		return fotoUrl;
	}
}
