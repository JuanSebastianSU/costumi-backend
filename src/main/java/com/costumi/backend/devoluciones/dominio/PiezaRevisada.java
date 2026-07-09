package com.costumi.backend.devoluciones.dominio;

import java.util.Objects;
import java.util.UUID;

/**
 * Ítem del checklist de devolución (RF-5.1): una unidad de una prenda concreta de la renta, si llegó
 * y con qué estado. El {@code prendaId} liga el daño/pérdida al artículo (grupo de stock) — RF-5.6. La
 * {@code descripcion} sirve para el número/QR de la pieza.
 *
 * <p><b>Piezas faltantes (RF-5.5):</b> una pieza que <b>no llegó</b> ({@code llego == false}) o que se
 * declara {@link EstadoPieza#PERDIDA} queda <b>pendiente</b> y <b>no cierra</b> la renta: se registra
 * en el checklist pero la unidad sigue afuera. Solo se considera <b>resuelta</b> cuando la pieza vuelve
 * en un estado aceptable, o cuando la pérdida se <b>cobra</b> ({@code perdidaCobrada == true}, se cobró
 * la reposición). Así el proceso no termina hasta devolver la prenda o marcarla perdida + cobrada.
 */
public class PiezaRevisada {

	private final UUID prendaId;
	private final String descripcion;
	private final boolean llego;
	private final EstadoPieza estado;
	private final boolean perdidaCobrada;

	private PiezaRevisada(UUID prendaId, String descripcion, boolean llego, EstadoPieza estado, boolean perdidaCobrada) {
		this.prendaId = Objects.requireNonNull(prendaId, "prendaId");
		if (descripcion == null || descripcion.isBlank()) {
			throw new IllegalArgumentException("La descripción de la pieza es obligatoria");
		}
		this.descripcion = descripcion.trim();
		this.llego = llego;
		this.estado = Objects.requireNonNull(estado, "estado");
		this.perdidaCobrada = perdidaCobrada;
	}

	public static PiezaRevisada de(UUID prendaId, String descripcion, boolean llego, EstadoPieza estado) {
		return new PiezaRevisada(prendaId, descripcion, llego, estado, false);
	}

	public static PiezaRevisada de(UUID prendaId, String descripcion, boolean llego, EstadoPieza estado,
			boolean perdidaCobrada) {
		return new PiezaRevisada(prendaId, descripcion, llego, estado, perdidaCobrada);
	}

	/** ¿La pieza falta? No llegó, o se declara perdida (RF-5.5). */
	public boolean estaFaltante() {
		return !llego || estado == EstadoPieza.PERDIDA;
	}

	/**
	 * ¿La pieza está resuelta? Volvió en un estado aceptable, o la pérdida ya se cobró (reposición).
	 * Solo las piezas resueltas consumen unidad, mueven inventario y cierran la renta.
	 */
	public boolean estaResuelta() {
		return !estaFaltante() || perdidaCobrada;
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

	public boolean perdidaCobrada() {
		return perdidaCobrada;
	}
}
