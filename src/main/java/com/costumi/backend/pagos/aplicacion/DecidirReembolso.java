package com.costumi.backend.pagos.aplicacion;

import com.costumi.backend.pagos.dominio.SolicitudDeReembolso;

/**
 * Puerto de entrada: la sucursal aprueba o rechaza una solicitud de reembolso con un motivo (paso 2).
 * Al aprobar ejecuta el reembolso (asiento + refund a tarjeta gateado). Revertir una rechazada exige un
 * rol superior en la pirámide (escalamiento).
 */
public interface DecidirReembolso {

	SolicitudDeReembolso ejecutar(DecidirReembolsoComando comando);
}
