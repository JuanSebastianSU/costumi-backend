package com.costumi.backend.catalogo.dominio;

import java.util.Objects;
import java.util.UUID;

/**
 * Tipo de etiqueta = dimensión de la taxonomía (ej. "Color", "Talla") (RF-2.7.1).
 * Pertenece a una Empresa (tenant). Lleva los interruptores de RF-2.7.2:
 * <ul>
 *   <li>{@code defineVariante}: ¿sus valores definen variantes de stock?</li>
 *   <li>{@code seleccionablePorCliente}: ¿el cliente puede elegirlo al personalizar?</li>
 * </ul>
 * Se <b>archiva</b> en vez de borrarse (RF-2.7.6).
 */
public class TipoEtiqueta {

	private final UUID id;
	private final UUID empresaId;
	private String nombre;
	private boolean defineVariante;
	private boolean seleccionablePorCliente;
	private boolean archivada;

	private TipoEtiqueta(UUID id, UUID empresaId, String nombre, boolean defineVariante,
			boolean seleccionablePorCliente, boolean archivada) {
		this.id = Objects.requireNonNull(id, "id");
		this.empresaId = Objects.requireNonNull(empresaId, "empresaId");
		this.nombre = exigirNombre(nombre);
		this.defineVariante = defineVariante;
		this.seleccionablePorCliente = seleccionablePorCliente;
		this.archivada = archivada;
	}

	public static TipoEtiqueta crear(UUID empresaId, String nombre, boolean defineVariante,
			boolean seleccionablePorCliente) {
		return new TipoEtiqueta(UUID.randomUUID(), empresaId, nombre, defineVariante, seleccionablePorCliente, false);
	}

	public static TipoEtiqueta rehidratar(UUID id, UUID empresaId, String nombre, boolean defineVariante,
			boolean seleccionablePorCliente, boolean archivada) {
		return new TipoEtiqueta(id, empresaId, nombre, defineVariante, seleccionablePorCliente, archivada);
	}

	public void archivar() {
		this.archivada = true;
	}

	public void renombrar(String nuevoNombre) {
		this.nombre = exigirNombre(nuevoNombre);
	}

	private static String exigirNombre(String nombre) {
		if (nombre == null || nombre.isBlank()) {
			throw new IllegalArgumentException("El nombre del tipo de etiqueta es obligatorio");
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

	public boolean defineVariante() {
		return defineVariante;
	}

	public boolean seleccionablePorCliente() {
		return seleccionablePorCliente;
	}

	public boolean archivada() {
		return archivada;
	}
}
