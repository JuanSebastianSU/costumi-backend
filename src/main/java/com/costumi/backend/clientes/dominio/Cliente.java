package com.costumi.backend.clientes.dominio;

import java.util.Objects;
import java.util.UUID;

/**
 * Cliente: ficha del cliente de una Empresa (tenant) (RF-7.1). Incluye datos de contacto y
 * documento de identidad (garantía), y un estado de confianza / lista negra (RF-7.3).
 */
public class Cliente {

	private final UUID id;
	private final UUID empresaId;
	private String nombre;
	private String telefono;
	private String email;
	private String documento;
	private String direccion;
	private boolean enListaNegra;
	private String deviceToken; // token de dispositivo para push FCM (RF-18.11); null si no registró
	/** Usuario del marketplace dueño de esta ficha (RF-14.4); null en fichas creadas por el personal. */
	private final UUID usuarioId;

	private Cliente(UUID id, UUID empresaId, String nombre, String telefono, String email, String documento,
			String direccion, boolean enListaNegra, UUID usuarioId) {
		this.id = Objects.requireNonNull(id, "id");
		this.empresaId = Objects.requireNonNull(empresaId, "empresaId");
		this.nombre = exigirNombre(nombre);
		this.telefono = limpiar(telefono);
		this.email = limpiar(email);
		this.documento = limpiar(documento);
		this.direccion = limpiar(direccion);
		this.enListaNegra = enListaNegra;
		this.usuarioId = usuarioId;
	}

	public static Cliente crear(UUID empresaId, String nombre, String telefono, String email, String documento,
			String direccion) {
		return new Cliente(UUID.randomUUID(), empresaId, nombre, telefono, email, documento, direccion, false, null);
	}

	/**
	 * Crea la ficha de cliente de un usuario del marketplace en una empresa (RF-14.4/18.5): la
	 * proyección por-tienda de una cuenta que vive a nivel plataforma.
	 */
	public static Cliente deUsuario(UUID empresaId, UUID usuarioId, String email) {
		String nombre = (email == null || email.isBlank()) ? "Cliente" : email.trim();
		return new Cliente(UUID.randomUUID(), empresaId, nombre, null, email, null, null, false, usuarioId);
	}

	public static Cliente rehidratar(UUID id, UUID empresaId, String nombre, String telefono, String email,
			String documento, String direccion, boolean enListaNegra, String deviceToken, UUID usuarioId) {
		Cliente cliente = new Cliente(id, empresaId, nombre, telefono, email, documento, direccion, enListaNegra,
				usuarioId);
		cliente.deviceToken = limpiar(deviceToken);
		return cliente;
	}

	/** Registra/actualiza el token de dispositivo para notificaciones push (RF-18.11). */
	public void registrarDeviceToken(String token) {
		this.deviceToken = limpiar(token);
	}

	public String deviceToken() {
		return deviceToken;
	}

	public void ponerEnListaNegra() {
		this.enListaNegra = true;
	}

	public void quitarDeListaNegra() {
		this.enListaNegra = false;
	}

	private static String exigirNombre(String nombre) {
		if (nombre == null || nombre.isBlank()) {
			throw new IllegalArgumentException("El nombre del cliente es obligatorio");
		}
		return nombre.trim();
	}

	private static String limpiar(String valor) {
		return (valor == null || valor.isBlank()) ? null : valor.trim();
	}

	public UUID id() {
		return id;
	}

	public UUID empresaId() {
		return empresaId;
	}

	public String nombre() {
		return nombre;
	}

	public String telefono() {
		return telefono;
	}

	public String email() {
		return email;
	}

	public String documento() {
		return documento;
	}

	public String direccion() {
		return direccion;
	}

	public boolean enListaNegra() {
		return enListaNegra;
	}

	/** Usuario del marketplace dueño de la ficha (null si la creó el personal). */
	public UUID usuarioId() {
		return usuarioId;
	}
}
