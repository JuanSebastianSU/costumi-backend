package com.costumi.backend.pedidos.adaptadores.entrada;

import com.costumi.backend.pedidos.dominio.TipoPedido;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** DTO de entrada para agregar un ítem al carrito. */
public record AgregarItemRequest(

		@NotNull(message = "La sucursal es obligatoria") UUID sucursalId,

		@NotNull(message = "El cliente es obligatorio") UUID clienteId,

		@NotNull(message = "El tipo (RENTA/VENTA) es obligatorio") TipoPedido tipo,

		@NotNull(message = "La prenda es obligatoria") UUID prendaId,

		@Min(value = 1, message = "La cantidad debe ser mayor a 0") int cantidad) {
}
