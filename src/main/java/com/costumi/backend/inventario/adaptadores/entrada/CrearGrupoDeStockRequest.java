package com.costumi.backend.inventario.adaptadores.entrada;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** DTO de entrada para crear un Grupo de stock. */
public record CrearGrupoDeStockRequest(

		@Size(max = 160, message = "La etiqueta no puede exceder 160 caracteres")
		String etiqueta,

		@Min(value = 0, message = "La cantidad inicial no puede ser negativa")
		int cantidadInicial) {
}
