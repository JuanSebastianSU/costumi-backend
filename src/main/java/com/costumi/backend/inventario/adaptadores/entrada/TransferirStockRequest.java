package com.costumi.backend.inventario.adaptadores.entrada;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** DTO de entrada para transferir unidades disponibles de un grupo de stock a otra sucursal (RF-10.3). */
public record TransferirStockRequest(

		@NotNull(message = "La sucursal de destino es obligatoria")
		UUID sucursalDestinoId,

		@Min(value = 1, message = "La cantidad a transferir debe ser mayor a 0")
		int cantidad) {
}
