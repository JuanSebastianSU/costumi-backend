package com.costumi.backend.inventario.adaptadores.entrada;

import jakarta.validation.constraints.Min;

/** DTO de entrada para reabastecer (entrada de mercancía) un grupo de stock (RF-10). */
public record EntradaDeStockRequest(

		@Min(value = 1, message = "La cantidad a ingresar debe ser mayor a 0")
		int cantidad) {
}
