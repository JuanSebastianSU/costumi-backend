package com.costumi.backend.pagos.aplicacion;

import com.costumi.backend.pagos.dominio.Pago;

/** Puerto de entrada: registrar un Pago (idempotente si se pasa clave) (RF-6.1/17.6). */
public interface RegistrarPago {

	Pago ejecutar(RegistrarPagoComando comando);
}
