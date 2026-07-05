package com.costumi.backend.inventario.adaptadores.entrada;

import com.costumi.backend.inventario.dominio.EstadoUnidad;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** DTO de entrada para mover unidades entre estados de un Grupo de stock. */
public record MoverUnidadesRequest(

		@NotNull(message = "El estado de origen es obligatorio")
		EstadoUnidad desde,

		@NotNull(message = "El estado de destino es obligatorio")
		EstadoUnidad hacia,

		@Min(value = 1, message = "La cantidad debe ser mayor a 0")
		int cantidad) {
}
