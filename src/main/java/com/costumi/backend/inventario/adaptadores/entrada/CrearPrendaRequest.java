package com.costumi.backend.inventario.adaptadores.entrada;

import com.costumi.backend.inventario.dominio.TipoArticulo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO de entrada para crear una Prenda. Los precios se validan según el tipo en el dominio.
 * {@code etiquetas} son los valores de etiqueta que la clasifican (RF-2.7); vacía/ausente = sin clasificar.
 */
public record CrearPrendaRequest(

		@NotNull(message = "La categoría es obligatoria")
		UUID categoriaId,

		@NotBlank(message = "El nombre de la prenda es obligatorio")
		@Size(max = 160, message = "El nombre no puede exceder 160 caracteres")
		String nombre,

		@NotNull(message = "El tipo de artículo es obligatorio")
		TipoArticulo tipoArticulo,

		BigDecimal precioRenta,

		BigDecimal precioVenta,

		BigDecimal costoAdquisicion,

		BigDecimal depositoSugerido,

		BigDecimal valorReposicion,

		BigDecimal valorDano,

		@Valid
		List<EtiquetaSeleccionadaDto> etiquetas) {

	public CrearPrendaRequest {
		etiquetas = (etiquetas == null) ? List.of() : etiquetas;
	}
}
