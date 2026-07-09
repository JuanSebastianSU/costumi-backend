package com.costumi.backend.catalogo.dominio;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Tipo de etiqueta = dimensión de la taxonomía (ej. "Color", "Talla") (RF-2.7.1).
 * Pertenece a una Empresa (tenant). Lleva los interruptores de RF-2.7.2:
 * <ul>
 *   <li>{@code defineVariante}: ¿sus valores definen variantes de stock?</li>
 *   <li>{@code seleccionablePorCliente}: ¿el cliente puede elegirlo al personalizar?</li>
 *   <li>{@code categoriasQueAplica}: ¿a qué categorías aplica? <b>Vacío = aplica a todas</b> (dimensión
 *       global como "Color"); con valores = solo esas categorías.</li>
 * </ul>
 * Se <b>archiva</b> en vez de borrarse (RF-2.7.6).
 */
public class TipoEtiqueta {

	private final UUID id;
	private final UUID empresaId;
	private String nombre;
	private boolean defineVariante;
	private boolean seleccionablePorCliente;
	private Set<UUID> categoriasQueAplica;
	private boolean archivada;

	private TipoEtiqueta(UUID id, UUID empresaId, String nombre, boolean defineVariante,
			boolean seleccionablePorCliente, Set<UUID> categoriasQueAplica, boolean archivada) {
		this.id = Objects.requireNonNull(id, "id");
		this.empresaId = Objects.requireNonNull(empresaId, "empresaId");
		this.nombre = exigirNombre(nombre);
		this.defineVariante = defineVariante;
		this.seleccionablePorCliente = seleccionablePorCliente;
		this.categoriasQueAplica = copiarCategorias(categoriasQueAplica);
		this.archivada = archivada;
	}

	public static TipoEtiqueta crear(UUID empresaId, String nombre, boolean defineVariante,
			boolean seleccionablePorCliente) {
		return crear(empresaId, nombre, defineVariante, seleccionablePorCliente, Set.of());
	}

	public static TipoEtiqueta crear(UUID empresaId, String nombre, boolean defineVariante,
			boolean seleccionablePorCliente, Set<UUID> categoriasQueAplica) {
		return new TipoEtiqueta(UUID.randomUUID(), empresaId, nombre, defineVariante, seleccionablePorCliente,
				categoriasQueAplica, false);
	}

	public static TipoEtiqueta rehidratar(UUID id, UUID empresaId, String nombre, boolean defineVariante,
			boolean seleccionablePorCliente, Set<UUID> categoriasQueAplica, boolean archivada) {
		return new TipoEtiqueta(id, empresaId, nombre, defineVariante, seleccionablePorCliente, categoriasQueAplica,
				archivada);
	}

	/** ¿Este tipo aplica a la categoría dada? (vacío = aplica a todas). */
	public boolean aplicaACategoria(UUID categoriaId) {
		return categoriasQueAplica.isEmpty() || categoriasQueAplica.contains(categoriaId);
	}

	private static Set<UUID> copiarCategorias(Set<UUID> categorias) {
		if (categorias == null || categorias.isEmpty()) {
			return Set.of();
		}
		Set<UUID> copia = new LinkedHashSet<>();
		for (UUID categoriaId : categorias) {
			if (categoriaId == null) {
				throw new IllegalArgumentException("Las categorías que aplican no pueden contener nulos");
			}
			copia.add(categoriaId);
		}
		return Collections.unmodifiableSet(copia);
	}

	public void archivar() {
		this.archivada = true;
	}

	/** Reactiva un tipo de etiqueta archivado. */
	public void activar() {
		this.archivada = false;
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

	public Set<UUID> categoriasQueAplica() {
		return categoriasQueAplica;
	}

	public boolean archivada() {
		return archivada;
	}
}
