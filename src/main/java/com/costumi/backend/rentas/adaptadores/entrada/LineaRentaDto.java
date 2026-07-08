package com.costumi.backend.rentas.adaptadores.entrada;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/** Un artículo del pedido de renta: prenda, cantidad y precio por día. */
public record LineaRentaDto(

		@NotNull(message = "La prenda es obligatoria") UUID prendaId,

		@Min(value = 1, message = "La cantidad debe ser al menos 1") int cantidad,

		@NotNull(message = "El precio por día es obligatorio")
		@Positive(message = "El precio por día debe ser mayor a 0") BigDecimal precioPorDia) {
}
