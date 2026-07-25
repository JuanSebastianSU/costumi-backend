package com.costumi.backend.identidad.dominio;

import java.time.LocalTime;

/**
 * Franja de atención de un día de la tienda (A7): a qué hora abre y cierra. {@code diaSemana} en formato
 * ISO (1=lunes … 7=domingo). Un día sin franja = la tienda no abre ese día. Value object del dominio.
 */
public record HorarioDeDia(int diaSemana, LocalTime abre, LocalTime cierra) {

	public HorarioDeDia {
		if (diaSemana < 1 || diaSemana > 7) {
			throw new IllegalArgumentException("El día de la semana debe estar entre 1 (lunes) y 7 (domingo)");
		}
		if (abre == null || cierra == null) {
			throw new IllegalArgumentException("El horario debe indicar hora de apertura y de cierre");
		}
		if (!cierra.isAfter(abre)) {
			throw new IllegalArgumentException("La hora de cierre debe ser posterior a la de apertura");
		}
	}
}
