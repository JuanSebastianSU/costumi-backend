package com.costumi.backend.disfraces.adaptadores.entrada;

import com.costumi.backend.disfraces.dominio.EjeDePrenda;
import com.costumi.backend.disfraces.dominio.EjeDeTalla;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Un slot de un disfraz por partes en la frontera HTTP. */
public record SlotDto(

		int orden,

		@NotBlank(message = "El nombre del slot es obligatorio")
		String nombre,

		@NotNull(message = "El eje de talla es obligatorio")
		EjeDeTalla ejeTalla,

		String tallaFija,

		@NotNull(message = "El eje de prenda es obligatorio")
		EjeDePrenda ejePrenda,

		UUID prendaFijaId,

		@Valid
		PoolDto pool,

		boolean opcional) {
}
