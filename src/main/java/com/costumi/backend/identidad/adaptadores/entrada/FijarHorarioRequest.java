package com.costumi.backend.identidad.adaptadores.entrada;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.List;

/** DTO de entrada para fijar el horario de atención de la tienda (A7): una franja por día que abre. */
public record FijarHorarioRequest(@Valid List<Franja> dias) {

	/** Una franja de un día. {@code diaSemana} ISO (1=lunes..7=domingo); horas en formato ISO (HH:mm[:ss]). */
	public record Franja(
			@Min(value = 1, message = "El día debe estar entre 1 y 7") @Max(value = 7, message = "El día debe estar entre 1 y 7")
			int diaSemana,
			@NotNull(message = "La hora de apertura es obligatoria") LocalTime abre,
			@NotNull(message = "La hora de cierre es obligatoria") LocalTime cierra) {
	}
}
