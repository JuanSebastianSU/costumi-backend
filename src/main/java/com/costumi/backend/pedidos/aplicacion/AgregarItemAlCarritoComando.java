package com.costumi.backend.pedidos.aplicacion;

import com.costumi.backend.pedidos.dominio.TipoPedido;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Datos para agregar un ítem al carrito pendiente de (empresa × sucursal × cliente × tipo). En los
 * carritos de RENTA, {@code fechaRetiro}/{@code fechaDevolucion} son el periodo del artículo (RF-18.6);
 * en los de VENTA son nulas.
 */
public record AgregarItemAlCarritoComando(UUID empresaId, UUID sucursalId, UUID clienteId, TipoPedido tipo,
		UUID prendaId, int cantidad, LocalDate fechaRetiro, LocalDate fechaDevolucion) {
}
