package com.costumi.backend.clientes.adaptadores.entrada;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO de entrada para guardar un disfraz como favorito (C4). Lleva el snapshot (nombre/foto/precios) que el
 * cliente ya tiene de la vitrina, para que "Mis guardados" se pinte sin resolver el disfraz en otro dispositivo.
 */
public record GuardarFavoritoRequest(

		@NotNull(message = "El id del disfraz es obligatorio")
		UUID disfrazId,

		@NotNull(message = "El id de la tienda es obligatorio")
		UUID empresaId,

		@NotBlank(message = "El nombre del disfraz es obligatorio")
		@Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
		String nombre,

		@Size(max = 500, message = "La URL de la foto no puede exceder 500 caracteres")
		String fotoUrl,

		BigDecimal precioRenta,

		BigDecimal precioVenta) {
}
