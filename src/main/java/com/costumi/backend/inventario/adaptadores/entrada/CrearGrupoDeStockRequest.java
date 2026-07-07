package com.costumi.backend.inventario.adaptadores.entrada;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * DTO de entrada para crear un Grupo de stock. Lleva la sucursal donde vive el stock (RF-18.2).
 * {@code combinacion} es la lista de selecciones de valor de etiqueta que define la variante; vacía/ausente
 * = variante única (prenda sin dimensiones).
 */
public record CrearGrupoDeStockRequest(

		@NotNull(message = "La sucursal es obligatoria")
		UUID sucursalId,

		@Valid
		List<SeleccionVarianteDto> combinacion,

		@Min(value = 0, message = "La cantidad inicial no puede ser negativa")
		int cantidadInicial) {

	public CrearGrupoDeStockRequest {
		combinacion = (combinacion == null) ? List.of() : combinacion;
	}
}
