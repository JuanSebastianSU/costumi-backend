package com.costumi.backend.inventario.adaptadores.entrada;

/**
 * Cuántas prendas activas dependen de un elemento de taxonomía (categoría / tipo / valor de etiqueta).
 * La UI lo usa para confirmar el impacto antes de archivar: "esto afecta a N prendas" (RF-2.8/2.7.6).
 */
record ConteoDeDependenciasResponse(int prendasActivas) {
}
