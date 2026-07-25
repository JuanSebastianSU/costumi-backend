package com.costumi.backend.marketplace.adaptadores.entrada;

import com.costumi.backend.marketplace.dominio.HorarioEnVitrina;

import java.time.LocalTime;

/** DTO de salida de una franja de horario de la tienda en la vitrina (A7). */
public record HorarioVitrinaResponse(int diaSemana, LocalTime abre, LocalTime cierra) {

	static HorarioVitrinaResponse desde(HorarioEnVitrina h) {
		return new HorarioVitrinaResponse(h.diaSemana(), h.abre(), h.cierra());
	}
}
