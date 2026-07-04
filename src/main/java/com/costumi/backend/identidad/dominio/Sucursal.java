package com.costumi.backend.identidad.dominio;

import java.util.Objects;
import java.util.UUID;

/**
 * Sucursal: local físico de una Empresa; ancla de inventario/ventas/reservas (RF-15.1, RF-14.1).
 *
 * <p>Toda Sucursal pertenece a una Empresa (tenant): lleva su {@code empresaId}. Agregado de
 * dominio puro, sin framework.
 */
public class Sucursal {

	private final UUID id;
	private final UUID empresaId;
	private String nombre;
	private String direccion;

	private Sucursal(UUID id, UUID empresaId, String nombre, String direccion) {
		this.id = Objects.requireNonNull(id, "id");
		this.empresaId = Objects.requireNonNull(empresaId, "empresaId");
		this.nombre = exigirNombre(nombre);
		this.direccion = (direccion == null || direccion.isBlank()) ? null : direccion.trim();
	}

	/** Crea una sucursal nueva para una empresa. */
	public static Sucursal crear(UUID empresaId, String nombre, String direccion) {
		return new Sucursal(UUID.randomUUID(), empresaId, nombre, direccion);
	}

	/** Reconstruye una Sucursal desde persistencia. */
	public static Sucursal rehidratar(UUID id, UUID empresaId, String nombre, String direccion) {
		return new Sucursal(id, empresaId, nombre, direccion);
	}

	private static String exigirNombre(String nombre) {
		if (nombre == null || nombre.isBlank()) {
			throw new IllegalArgumentException("El nombre de la sucursal es obligatorio");
		}
		return nombre.trim();
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

	public String direccion() {
		return direccion;
	}
}
