package com.costumi.backend.devoluciones.dominio;

import java.util.Objects;

/** Ítem del checklist de devolución (RF-5.1): una pieza, si llegó y con qué estado. */
public class PiezaRevisada {

	private final String descripcion;
	private final boolean llego;
	private final EstadoPieza estado;

	private PiezaRevisada(String descripcion, boolean llego, EstadoPieza estado) {
		if (descripcion == null || descripcion.isBlank()) {
			throw new IllegalArgumentException("La descripción de la pieza es obligatoria");
		}
		this.descripcion = descripcion.trim();
		this.llego = llego;
		this.estado = Objects.requireNonNull(estado, "estado");
	}

	public static PiezaRevisada de(String descripcion, boolean llego, EstadoPieza estado) {
		return new PiezaRevisada(descripcion, llego, estado);
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
