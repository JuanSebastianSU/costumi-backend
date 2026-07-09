package com.costumi.backend.inventario.adaptadores.entrada;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de entrada para editar una Prenda (RF-2.10). La categoría y el {@code tipoArticulo} no cambian
 * (para cambiarlos, archívala y crea una nueva); los precios se validan según el tipo existente.
 */
public record EditarPrendaRequest(

		@NotBlank(message = "El nombre de la prenda es obligatorio")
		@Size(max = 160, message = "El nombre no puede exceder 160 caracteres")
		String nombre,

		BigDecimal precioRenta,

		BigDecimal precioVenta,

		BigDecimal costoAdquisicion,

		BigDecimal depositoSugerido,

		BigDecimal valorReposicion,

		BigDecimal valorDano,

		@Valid
		List<EtiquetaSeleccionadaDto> etiquetas) {

	public EditarPrendaRequest {
		etiquetas = (etiquetas == null) ? List.of() : etiquetas;
	}
}
