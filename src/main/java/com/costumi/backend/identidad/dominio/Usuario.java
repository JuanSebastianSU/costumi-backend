package com.costumi.backend.identidad.dominio;

import java.util.Objects;
import java.util.UUID;

/**
 * Usuario del sistema (RF-1). Pertenece a una Empresa (tenant), salvo el SuperAdmin, que
 * es de plataforma y no tiene empresa. Agregado de dominio puro.
 *
 * <p>Guarda el <b>hash</b> de la contraseña (opaco); el hashing/verificación con BCrypt es
 * responsabilidad de la capa de aplicación/adaptadores, no del dominio.
 */
public class Usuario {

	private final UUID id;
	private final UUID empresaId;
	private final String email;
	private final String passwordHash;
	private final Rol rol;

	private Usuario(UUID id, UUID empresaId, String email, String passwordHash, Rol rol) {
		this.id = Objects.requireNonNull(id, "id");
		this.rol = Objects.requireNonNull(rol, "rol");
		this.email = exigir(email, "email");
		this.passwordHash = exigir(passwordHash, "passwordHash");
		this.empresaId = validarTenant(empresaId, rol);
	}

	public static Usuario crear(UUID empresaId, String email, String passwordHash, Rol rol) {
		return new Usuario(UUID.randomUUID(), empresaId, email, passwordHash, rol);
	}

	public static Usuario rehidratar(UUID id, UUID empresaId, String email, String passwordHash, Rol rol) {
		return new Usuario(id, empresaId, email, passwordHash, rol);
	}

	private static UUID validarTenant(UUID empresaId, Rol rol) {
		if (!rol.requiereEmpresa() && empresaId != null) {
			throw new IllegalArgumentException("El rol " + rol + " no pertenece a ninguna empresa");
		}
		if (rol.requiereEmpresa() && empresaId == null) {
			throw new IllegalArgumentException("El usuario debe pertenecer a una empresa");
		}
		return empresaId;
	}

	private static String exigir(String valor, String campo) {
		if (valor == null || valor.isBlank()) {
			throw new IllegalArgumentException("El campo " + campo + " es obligatorio");
		}
		return valor.trim();
	}

	public UUID id() {
		return id;
	}

	public UUID empresaId() {
		return empresaId;
	}

	public String email() {
		return email;
	}

	public String passwordHash() {
		return passwordHash;
	}

	public Rol rol() {
		return rol;
	}
}
