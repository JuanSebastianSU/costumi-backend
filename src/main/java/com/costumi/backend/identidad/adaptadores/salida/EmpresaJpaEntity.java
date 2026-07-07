package com.costumi.backend.identidad.adaptadores.salida;

import com.costumi.backend.identidad.dominio.EstadoEmpresa;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Mapeo JPA de la Empresa. Vive en el adaptador de salida; nunca se expone por la API. */
@Entity
@Table(name = "empresa")
class EmpresaJpaEntity {

	@Id
	private UUID id;

	@Column(nullable = false, length = 200)
	private String nombre;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EstadoEmpresa estado;

	@Column(name = "fecha_registro", nullable = false)
	private Instant fechaRegistro;

	@Column(length = 300)
	private String ubicacion;

	@Column(length = 200)
	private String contacto;

	@Column(name = "solicitante_id")
	private UUID solicitanteId;

	protected EmpresaJpaEntity() {
		// requerido por JPA
	}

	EmpresaJpaEntity(UUID id, String nombre, EstadoEmpresa estado, Instant fechaRegistro,
			String ubicacion, String contacto, UUID solicitanteId) {
		this.id = id;
		this.nombre = nombre;
		this.estado = estado;
		this.fechaRegistro = fechaRegistro;
		this.ubicacion = ubicacion;
		this.contacto = contacto;
		this.solicitanteId = solicitanteId;
	}

	UUID getId() {
		return id;
	}

	String getNombre() {
		return nombre;
	}

	EstadoEmpresa getEstado() {
		return estado;
	}

	Instant getFechaRegistro() {
		return fechaRegistro;
	}

	String getUbicacion() {
		return ubicacion;
	}

	String getContacto() {
		return contacto;
	}

	UUID getSolicitanteId() {
		return solicitanteId;
	}
}
