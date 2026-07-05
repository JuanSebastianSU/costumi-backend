package com.costumi.backend.inventario.aplicacion;

import java.util.UUID;

/** Datos para crear un Grupo de stock de una Prenda con su cantidad inicial disponible. */
public record CrearGrupoDeStockComando(UUID empresaId, UUID prendaId, String etiqueta, int cantidadInicial) {
}
