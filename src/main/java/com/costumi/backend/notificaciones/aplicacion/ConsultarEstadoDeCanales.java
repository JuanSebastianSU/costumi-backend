package com.costumi.backend.notificaciones.aplicacion;

/** Puerto de entrada: estado de los canales externos (para diagnosticar envíos que caen al log). */
public interface ConsultarEstadoDeCanales {

	EstadoDeCanales estado();

	/** Manda una push de prueba al dispositivo del cliente y devuelve si salio y, si no, por que. */
	ResultadoDePrueba probarPush(java.util.UUID empresaId, java.util.UUID clienteId);
}
