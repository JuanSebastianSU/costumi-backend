package com.costumi.backend.identidad.adaptadores.salida;

import com.costumi.backend.compartido.FiltroTenant;
import org.hibernate.annotations.Filter;

import com.costumi.backend.identidad.dominio.Rol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Mapeo JPA del Usuario. {@code empresa_id} nulo para el SuperAdmin (plataforma). */
@Entity
@Table(name = "usuario")
@Filter(name = FiltroTenant.NOMBRE)
class UsuarioJpaEntity {

	@Id
	private UUID id;

	@Column(name = "empresa_id")
	private UUID empresaId;

	@Column(nullable = false, unique = true, length = 320)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Rol rol;

	@Column(nullable = false)
	private boolean activo;

	/** Datos del perfil que administra el propio usuario (RF-14); opcionales. */
	@Column(length = 120)
	private String nombre;

	@Column(length = 40)
	private String telefono;

	protected UsuarioJpaEntity() {
		// requerido por JPA
	}

	UsuarioJpaEntity(UUID id, UUID empresaId, String email, String passwordHash, Rol rol, boolean activo,
			String nombre, String telefono) {
		this.id = id;
		this.empresaId = empresaId;
		this.email = email;
		this.passwordHash = passwordHash;
		this.rol = rol;
		this.activo = activo;
		this.nombre = nombre;
		this.telefono = telefono;
	}

	String getNombre() {
		return nombre;
	}

	String getTelefono() {
		return telefono;
	}

	UUID getId() {
		return id;
	}

	UUID getEmpresaId() {
		return empresaId;
	}

	String getEmail() {
		return email;
	}

	String getPasswordHash() {
		return passwordHash;
	}

	boolean isActivo() {
		return activo;
	}

	Rol getRol() {
		return rol;
	}
}
