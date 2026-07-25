package com.costumi.backend.identidad.adaptadores.salida;

import com.costumi.backend.identidad.dominio.EstadoInvitacion;
import com.costumi.backend.identidad.dominio.Rol;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Mapeo JPA de la invitación de trabajo (Fase B). SIN filtro de tenant: la aceptación es pública (por token),
 * la creación/listado ya se acotan por empresa en el service. Las sucursales van en la tabla hija.
 */
@Entity
@Table(name = "invitacion")
class InvitacionJpaEntity {

	@Id
	private UUID id;

	@Column(name = "empresa_id", nullable = false)
	private UUID empresaId;

	@Column(nullable = false, length = 320)
	private String email;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Rol rol;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "invitacion_sucursal", joinColumns = @JoinColumn(name = "invitacion_id"))
	@Column(name = "sucursal_id", nullable = false)
	private Set<UUID> sucursalIds = new HashSet<>();

	@Column(name = "token_hash", nullable = false, length = 64)
	private String tokenHash;

	@Column(name = "expira_en", nullable = false)
	private Instant expiraEn;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EstadoInvitacion estado;

	@Column(name = "acepto_terminos_en")
	private Instant aceptoTerminosEn;

	protected InvitacionJpaEntity() {
		// requerido por JPA
	}

	InvitacionJpaEntity(UUID id, UUID empresaId, String email, Rol rol, Set<UUID> sucursalIds, String tokenHash,
			Instant expiraEn, EstadoInvitacion estado, Instant aceptoTerminosEn) {
		this.id = id;
		this.empresaId = empresaId;
		this.email = email;
		this.rol = rol;
		this.sucursalIds = new HashSet<>(sucursalIds);
		this.tokenHash = tokenHash;
		this.expiraEn = expiraEn;
		this.estado = estado;
		this.aceptoTerminosEn = aceptoTerminosEn;
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

	Rol getRol() {
		return rol;
	}

	Set<UUID> getSucursalIds() {
		return sucursalIds;
	}

	String getTokenHash() {
		return tokenHash;
	}

	Instant getExpiraEn() {
		return expiraEn;
	}

	EstadoInvitacion getEstado() {
		return estado;
	}

	Instant getAceptoTerminosEn() {
		return aceptoTerminosEn;
	}
}
