package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.identidad.dominio.HorarioDeDia;

import java.time.LocalTime;

/** DTO de salida de una franja de horario de atención (A7). {@code diaSemana} ISO (1=lunes..7=domingo). */
public record HorarioResponse(int diaSemana, LocalTime abre, LocalTime cierra) {

	static HorarioResponse desde(HorarioDeDia h) {
		return new HorarioResponse(h.diaSemana(), h.abre(), h.cierra());
	}
}
