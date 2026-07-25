package com.costumi.backend.rentas.dominio;

/** Conteo de rentas por bandeja (para los números de las pestañas de G9). */
public record ResumenDeRentas(long porEntregar, long activas, long vencidas, long cerradas) {
}
