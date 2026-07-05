package com.costumi.backend.catalogo.dominio;

import java.util.Objects;
import java.util.UUID;

/**
 * Categoría: la "parte del cuerpo" (camisa, pantalón, sombrero…), editable por el dueño (RF-2.8).
 * Pertenece a una Empresa (tenant). Se <b>archiva</b> en vez de borrarse (RF-2.7.6).
 */
public class Categoria {

	private final UUID id;
	private final UUID empresaId;
	private String nombre;
	private boolean archivada;

	private Categoria(UUID id, UUID empresaId, String nombre, boolean archivada) {
		this.id = Objects.requireNonNull(id, "id");
		this.empresaId = Objects.requireNonNull(empresaId, "empresaId");
		this.nombre = exigirNombre(nombre);
		this.archivada = archivada;
	}

	public static Categoria crear(UUID empresaId, String nombre) {
		return new Categoria(UUID.randomUUID(), empresaId, nombre, false);
	}

	public static Categoria rehidratar(UUID id, UUID empresaId, String nombre, boolean archivada) {
		return new Categoria(id, empresaId, nombre, archivada);
	}

	/** Archiva la categoría (RF-2.7.6): deja de usarse pero conserva el histórico. */
	public void archivar() {
		this.archivada = true;
	}

	/** Renombrar propaga (RF-2.7.6): es la misma categoría con otro nombre. */
	public void renombrar(String nuevoNombre) {
		this.nombre = exigirNombre(nuevoNombre);
	}

	private static String exigirNombre(String nombre) {
		if (nombre == null || nombre.isBlank()) {
			throw new IllegalArgumentException("El nombre de la categoría es obligatorio");
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

	public boolean archivada() {
		return archivada;
	}
}
