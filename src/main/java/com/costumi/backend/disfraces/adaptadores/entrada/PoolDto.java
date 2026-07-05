package com.costumi.backend.disfraces.adaptadores.entrada;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/** Pool de un slot personalizable: categoría + valores de etiqueta permitidos por dimensión. */
public record PoolDto(

		@NotNull(message = "La categoría del pool es obligatoria")
		UUID categoriaId,

		@Valid
		List<EtiquetaPermitidaDto> etiquetasPermitidas) {

	public PoolDto {
		etiquetasPermitidas = (etiquetasPermitidas == null) ? List.of() : etiquetasPermitidas;
	}
}
