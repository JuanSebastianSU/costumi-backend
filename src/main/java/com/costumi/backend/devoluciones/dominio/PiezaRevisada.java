package com.costumi.backend.devoluciones.dominio;

import java.util.Objects;
import java.util.UUID;

/**
 * Ítem del checklist de devolución (RF-5.1): una unidad de una prenda concreta de la renta, si llegó
 * y con qué estado. El {@code prendaId} liga el daño/pérdida al artículo (grupo de stock) — RF-5.6. La
 * {@code descripcion} sirve para el número/QR de la pieza.
 */
public class PiezaRevisada {

	private final UUID prendaId;
	private final String descripcion;
	private final boolean llego;
	private final EstadoPieza estado;

	private PiezaRevisada(UUID prendaId, String descripcion, boolean llego, EstadoPieza estado) {
		this.prendaId = Objects.requireNonNull(prendaId, "prendaId");
		if (descripcion == null || descripcion.isBlank()) {
			throw new IllegalArgumentException("La descripción de la pieza es obligatoria");
		}
		this.descripcion = descripcion.trim();
		this.llego = llego;
		this.estado = Objects.requireNonNull(estado, "estado");
	}

	public static PiezaRevisada de(UUID prendaId, String descripcion, boolean llego, EstadoPieza estado) {
		return new PiezaRevisada(prendaId, descripcion, llego, estado);
	}

	public UUID prendaId() {
		return prendaId;
	}

	public String descripcion() {
		return descripcion;
	}

	public boolean llego() {
		return llego;
	}

	public EstadoPieza estado() {
		return estado;
	}
}
