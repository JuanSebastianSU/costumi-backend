package com.costumi.backend.inventario.adaptadores.entrada;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Una etiqueta de la prenda en la frontera HTTP: {@code tipo -> valor} de etiqueta. */
public record EtiquetaSeleccionadaDto(

		@NotNull(message = "El tipo de etiqueta es obligatorio")
		UUID tipoEtiquetaId,

		@NotNull(message = "El valor de etiqueta es obligatorio")
		UUID valorEtiquetaId) {
}
