package com.costumi.backend.rentas.dominio;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Periodo de una renta (RF-3.2): fecha de retiro y de devolución. Value object inmutable con la lógica
 * de <b>traslape</b> que sostiene la disponibilidad por fechas.
 *
 * <p>El traslape es <b>inclusivo</b> en los extremos: si una renta devuelve el día X y otra retira el
 * día X, se consideran traslapadas (no se asume rotación el mismo día — decisión de negocio, ver PROGRESS).
 */
public record Periodo(LocalDate retiro, LocalDate devolucion) {

	public Periodo {
		Objects.requireNonNull(retiro, "retiro");
		Objects.requireNonNull(devolucion, "devolucion");
		if (devolucion.isBefore(retiro)) {
			throw new IllegalArgumentException("La fecha de devolución no puede ser anterior a la de retiro");
		}
	}

	/** ¿Se solapa con otro periodo? (extremos inclusivos). */
	public boolean seSolapaCon(Periodo otro) {
		return !retiro.isAfter(otro.devolucion) && !otro.retiro.isAfter(devolucion);
	}

	/** Días del periodo, mínimo 1. */
	public long dias() {
		return Math.max(1, ChronoUnit.DAYS.between(retiro, devolucion));
	}
}
