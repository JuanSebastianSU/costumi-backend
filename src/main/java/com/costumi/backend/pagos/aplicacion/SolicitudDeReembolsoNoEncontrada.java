package com.costumi.backend.pagos.aplicacion;

import java.util.UUID;

/** No existe una solicitud de reembolso con ese id en la empresa (404). */
public class SolicitudDeReembolsoNoEncontrada extends RuntimeException {

	public SolicitudDeReembolsoNoEncontrada(UUID id) {
		super("No existe la solicitud de reembolso " + id);
	}
}
