package com.costumi.backend.disfraces.dominio;

import java.util.Objects;
import java.util.UUID;

/**
 * Categoría de DISFRAZ: el tema/agrupación que el dueño usa para sus disfraces (ej. "Piratas",
 * "Superhéroes"). Es <b>independiente</b> de las categorías de prenda (Camisa, Pantalón…): son dos
 * taxonomías distintas. Pertenece a una Empresa (tenant) y se <b>archiva</b> en vez de borrarse.
 */
public class CategoriaDeDisfraz {

	private final UUID id;
	private final UUID empresaId;
	private String nombre;
	private boolean archivada;

	private CategoriaDeDisfraz(UUID id, UUID empresaId, String nombre, boolean archivada) {
		this.id = Objects.requireNonNull(id, "id");
		this.empresaId = Objects.requireNonNull(empresaId, "empresaId");
		this.nombre = exigirNombre(nombre);
		this.archivada = archivada;
	}

	public static CategoriaDeDisfraz crear(UUID empresaId, String nombre) {
		return new CategoriaDeDisfraz(UUID.randomUUID(), empresaId, nombre, false);
	}

	public static CategoriaDeDisfraz rehidratar(UUID id, UUID empresaId, String nombre, boolean archivada) {
		return new CategoriaDeDisfraz(id, empresaId, nombre, archivada);
	}

	public void archivar() {
		this.archivada = true;
	}

	public void activar() {
		this.archivada = false;
	}

	public void renombrar(String nuevoNombre) {
		this.nombre = exigirNombre(nuevoNombre);
	}

	private static String exigirNombre(String nombre) {
		if (nombre == null || nombre.isBlank()) {
			throw new IllegalArgumentException("El nombre de la categoría de disfraz es obligatorio");
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
