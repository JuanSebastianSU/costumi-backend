package com.costumi.backend.catalogo.dominio;

import java.util.Objects;
import java.util.UUID;

/**
 * Valor de una dimensión (ej. "Rojo" del tipo "Color") (RF-2.7.1). Pertenece a un
 * {@link TipoEtiqueta} y lleva su {@code empresaId} (tenant). Se archiva, no se borra (RF-2.7.6).
 */
public class ValorEtiqueta {

	private final UUID id;
	private final UUID empresaId;
	private final UUID tipoEtiquetaId;
	private String valor;
	private boolean archivada;

	private ValorEtiqueta(UUID id, UUID empresaId, UUID tipoEtiquetaId, String valor, boolean archivada) {
		this.id = Objects.requireNonNull(id, "id");
		this.empresaId = Objects.requireNonNull(empresaId, "empresaId");
		this.tipoEtiquetaId = Objects.requireNonNull(tipoEtiquetaId, "tipoEtiquetaId");
		this.valor = exigirValor(valor);
		this.archivada = archivada;
	}

	public static ValorEtiqueta crear(UUID empresaId, UUID tipoEtiquetaId, String valor) {
		return new ValorEtiqueta(UUID.randomUUID(), empresaId, tipoEtiquetaId, valor, false);
	}

	public static ValorEtiqueta rehidratar(UUID id, UUID empresaId, UUID tipoEtiquetaId, String valor,
			boolean archivada) {
		return new ValorEtiqueta(id, empresaId, tipoEtiquetaId, valor, archivada);
	}

	public void archivar() {
		this.archivada = true;
	}

	/** Reactiva un valor de etiqueta archivado. */
	public void activar() {
		this.archivada = false;
	}

	/** Renombra el valor (RF-2.7.6). Como las prendas/variantes guardan el id, el cambio propaga. */
	public void renombrar(String nuevoValor) {
		this.valor = exigirValor(nuevoValor);
	}

	private static String exigirValor(String valor) {
		if (valor == null || valor.isBlank()) {
			throw new IllegalArgumentException("El valor de etiqueta es obligatorio");
		}
		return valor.trim();
	}

	public UUID id() {
		return id;
	}

	public UUID empresaId() {
		return empresaId;
	}

	public UUID tipoEtiquetaId() {
		return tipoEtiquetaId;
	}

	public String valor() {
		return valor;
	}

	public boolean archivada() {
		return archivada;
	}
}
