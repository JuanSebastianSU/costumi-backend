package com.costumi.backend.ventas.aplicacion;

import com.costumi.backend.ventas.dominio.Venta;

import java.util.UUID;

/**
 * Puerto de entrada: devolución de una venta (RF-4.5). Marca la venta como DEVUELTA y reingresa el
 * stock de sus líneas. El reintegro del dinero se registra aparte como un pago de tipo REEMBOLSO.
 */
public interface DevolverVenta {

	Venta devolver(UUID empresaId, UUID ventaId);
}
