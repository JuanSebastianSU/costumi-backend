package com.costumi.backend.pagos.dominio;

/** Estado de una solicitud de reembolso (RF-4.5/6.9). */
public enum EstadoSolicitudReembolso {

	/** Esperando la decisión de la sucursal. */
	PENDIENTE,

	/** Aprobada: se ejecutó el reembolso (asiento y, si el pago fue con tarjeta, refund en la pasarela). Terminal. */
	APROBADA,

	/** Rechazada con motivo. Un rol superior en la pirámide puede revertirla (escalamiento). */
	RECHAZADA
}
