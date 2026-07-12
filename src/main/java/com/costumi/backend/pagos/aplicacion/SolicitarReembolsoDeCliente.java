package com.costumi.backend.pagos.aplicacion;

import com.costumi.backend.pagos.dominio.SolicitudDeReembolso;

/**
 * Puerto de entrada: un CLIENTE del marketplace solicita el reembolso de <b>su propia</b> venta o renta desde
 * su cuenta (RF-4.5/14.4). Verifica la propiedad de la operación antes de crear la solicitud; el resto del
 * flujo (la sucursal decide) es idéntico al de una solicitud registrada por el personal.
 */
public interface SolicitarReembolsoDeCliente {

	SolicitudDeReembolso ejecutar(SolicitarReembolsoDeClienteComando comando);
}
