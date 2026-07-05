package com.costumi.backend.disfraces.adaptadores.entrada;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/** Una dimensión del pool de un slot: el tipo y los valores permitidos para el cliente. */
public record EtiquetaPermitidaDto(

		@NotNull(message = "El tipo de etiqueta es obligatorio")
		UUID tipoEtiquetaId,

		@NotEmpty(message = "Debe indicar al menos un valor permitido")
		List<UUID> valores) {
}
