package com.costumi.backend.reportes.dominio;

import java.util.UUID;

/**
 * Fila del tablero de estado de inventario (RF-9.3): por grupo de stock, cuántas piezas hay en cada
 * estado (disponibles/rentadas/dañadas/en limpieza/perdidas).
 */
public record GrupoInventario(UUID prendaId, String prendaNombre, int disponibles, int rentadas, int danadas,
		int enLimpieza, int perdidas) {
}
