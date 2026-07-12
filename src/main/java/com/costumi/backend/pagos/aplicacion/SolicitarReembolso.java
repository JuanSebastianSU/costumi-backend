package com.costumi.backend.pagos.aplicacion;

import com.costumi.backend.pagos.dominio.SolicitudDeReembolso;

/** Puerto de entrada: crea una solicitud de reembolso (paso 1 del proceso, RF-4.5/6.9). */
public interface SolicitarReembolso {

	SolicitudDeReembolso ejecutar(SolicitarReembolsoComando comando);
}
