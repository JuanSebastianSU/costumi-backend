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
	private final boolean activo;
	/** Datos que el propio usuario administra (RF-14). Opcionales: puede no haberlos cargado todavía. */
	private final String nombre;
	private final String telefono;

	private Usuario(UUID id, UUID empresaId, String email, String passwordHash, Rol rol, boolean activo,
			String nombre, String telefono) {
		this.id = Objects.requireNonNull(id, "id");
		this.rol = Objects.requireNonNull(rol, "rol");
		this.email = exigir(email, "email");
		this.passwordHash = exigir(passwordHash, "passwordHash");
		this.empresaId = validarTenant(empresaId, rol);
		this.activo = activo;
		this.nombre = normalizar(nombre);
		this.telefono = normalizar(telefono);
	}

	public static Usuario crear(UUID empresaId, String email, String passwordHash, Rol rol) {
		return new Usuario(UUID.randomUUID(), empresaId, email, passwordHash, rol, true, null, null);
	}

	public static Usuario rehidratar(UUID id, UUID empresaId, String email, String passwordHash, Rol rol) {
		return rehidratar(id, empresaId, email, passwordHash, rol, true);
	}

	public static Usuario rehidratar(UUID id, UUID empresaId, String email, String passwordHash, Rol rol,
			boolean activo) {
		return new Usuario(id, empresaId, email, passwordHash, rol, activo, null, null);
	}

	public static Usuario rehidratar(UUID id, UUID empresaId, String email, String passwordHash, Rol rol,
			boolean activo, String nombre, String telefono) {
		return new Usuario(id, empresaId, email, passwordHash, rol, activo, nombre, telefono);
	}

	/**
	 * El propio usuario actualiza sus datos (RF-14). Vacío borra el dato: son opcionales. No toca el
	 * correo, que identifica la cuenta, ni el rol.
	 */
	public Usuario actualizarPerfil(String nuevoNombre, String nuevoTelefono) {
		return new Usuario(id, empresaId, email, passwordHash, rol, activo, nuevoNombre, nuevoTelefono);
	}

	/** Nombre para mostrar: el que cargó, o su correo si todavía no puso ninguno. */
	public String nombreParaMostrar() {
		return nombre == null ? email : nombre;
	}

	/**
	 * Promueve un CLIENTE a DUEÑO de la empresa recién aprobada por el SuperAdmin (marketplace).
	 * Es la <b>misma cuenta</b> (mismo id/email/contraseña); solo cambia el rol y se le asigna la empresa.
	 */
	public Usuario promoverADueno(UUID empresaId) {
		if (!rol.esCliente()) {
			throw new IllegalStateException("Solo un CLIENTE puede promoverse a DUEÑO");
		}
		return new Usuario(id, empresaId, email, passwordHash, Rol.DUENO, activo, nombre, telefono);
	}

	/** Cambia la contraseña (recibe el hash ya cifrado). Misma cuenta: solo cambia el hash. */
	public Usuario cambiarContrasena(String nuevoPasswordHash) {
		return new Usuario(id, empresaId, email, nuevoPasswordHash, rol, activo, nombre, telefono);
	}

	/** Da de baja al usuario: no podrá autenticarse ni renovar sesión (RF-8). Misma cuenta, se conserva. */
	public Usuario desactivar() {
		return new Usuario(id, empresaId, email, passwordHash, rol, false, nombre, telefono);
	}

	/** Reactiva al usuario dado de baja. */
	public Usuario activar() {
		return new Usuario(id, empresaId, email, passwordHash, rol, true, nombre, telefono);
	}

	/**
	 * Cambia el rol del empleado (RF-8, G2). Misma cuenta (id/email/contraseña/estado). {@code validarTenant}
	 * rechaza pasar a un rol que no pertenece a una empresa (SuperAdmin/Cliente): un empleado sigue siendo
	 * personal de su empresa. La autoridad para hacerlo (pirámide) la exige la capa de aplicación.
	 */
	public Usuario cambiarRol(Rol nuevoRol) {
		return new Usuario(id, empresaId, email, passwordHash, nuevoRol, activo, nombre, telefono);
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

	/** Un texto en blanco es lo mismo que no tener el dato. */
	private static String normalizar(String valor) {
		return valor == null || valor.isBlank() ? null : valor.trim();
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

	public boolean activo() {
		return activo;
	}

	public String nombre() {
		return nombre;
	}

	public String telefono() {
		return telefono;
	}
}
