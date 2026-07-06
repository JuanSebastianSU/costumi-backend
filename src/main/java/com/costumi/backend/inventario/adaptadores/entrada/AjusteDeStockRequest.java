package com.costumi.backend.inventario.adaptadores.entrada;

import com.costumi.backend.inventario.dominio.EstadoUnidad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** DTO de entrada para un ajuste de inventario con motivo (RF-10). {@code delta} puede ser ±. */
public record AjusteDeStockRequest(

		@NotNull(message = "El estado a ajustar es obligatorio") EstadoUnidad estado,

		int delta,

		@NotBlank(message = "El motivo del ajuste es obligatorio") String motivo) {
}
