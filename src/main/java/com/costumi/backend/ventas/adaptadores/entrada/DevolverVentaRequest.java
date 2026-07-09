package com.costumi.backend.ventas.adaptadores.entrada;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.UUID;

/**
 * DTO de entrada para devolver una venta (RF-4.5). Si es {@code null} o {@code lineas} está vacío, se
 * devuelve todo lo pendiente (total); si trae líneas, se devuelven solo esas unidades (parcial).
 */
public record DevolverVentaRequest(List<LineaADevolver> lineas) {

	public record LineaADevolver(

			@NotNull(message = "La prenda es obligatoria") UUID prendaId,

			@Positive(message = "La cantidad a devolver debe ser mayor a 0") int cantidad) {
	}
}
