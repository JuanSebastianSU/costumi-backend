package com.costumi.backend.disfraces.adaptadores.entrada;

import com.costumi.backend.disfraces.dominio.EjeDePrenda;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Un slot de un disfraz en la frontera HTTP: fijo ({@code prendaFijaId}) o personalizable. Un slot
 * personalizable define sus opciones con {@code prendasOpcion} (prendas explícitas elegidas del
 * inventario) o, por compatibilidad, con {@code pool} (categoría + etiquetas).
 */
public record SlotDto(

		int orden,

		@NotBlank(message = "El nombre del slot es obligatorio")
		String nombre,

		@NotNull(message = "El eje de prenda es obligatorio")
		EjeDePrenda ejePrenda,

		UUID prendaFijaId,

		@Valid
		PoolDto pool,

		List<UUID> prendasOpcion,

		boolean opcional) {

	public SlotDto {
		prendasOpcion = (prendasOpcion == null) ? List.of() : prendasOpcion;
	}
}
