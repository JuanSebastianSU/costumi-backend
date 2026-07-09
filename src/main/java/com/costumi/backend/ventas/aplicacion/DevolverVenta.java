package com.costumi.backend.ventas.aplicacion;

import com.costumi.backend.ventas.dominio.Venta;

import java.util.Map;
import java.util.UUID;

/**
 * Puerto de entrada: devolución de una venta (RF-4.5). Devuelve las unidades indicadas por prenda
 * ({@code cantidades}); {@code null}/vacío = todo lo pendiente (total). Marca la venta como
 * PARCIALMENTE_DEVUELTA o DEVUELTA, reingresa el stock de lo devuelto y respeta la política de
 * reembolso del local. El reintegro del dinero se registra aparte como un pago de tipo REEMBOLSO.
 */
public interface DevolverVenta {

	Venta devolver(UUID empresaId, UUID ventaId, Map<UUID, Integer> cantidades);
}
