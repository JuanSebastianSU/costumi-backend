package com.costumi.backend.marketplace.dominio;

import java.time.LocalTime;

/** Franja de horario de atención de una tienda en la vitrina (A7). {@code diaSemana} ISO (1=lunes..7=domingo). */
public record HorarioEnVitrina(int diaSemana, LocalTime abre, LocalTime cierra) {
}
