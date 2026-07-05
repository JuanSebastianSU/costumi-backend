package com.costumi.backend.ventas.aplicacion;

import com.costumi.backend.ventas.dominio.Venta;

/** Puerto de entrada: registrar una Venta de POS (RF-4). */
public interface RegistrarVenta {

	Venta ejecutar(RegistrarVentaComando comando);
}
