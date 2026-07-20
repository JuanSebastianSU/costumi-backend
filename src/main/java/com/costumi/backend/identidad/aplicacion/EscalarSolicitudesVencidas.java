package com.costumi.backend.identidad.aplicacion;

/**
 * Puerto de entrada (RF-15.4): escala las solicitudes de empresa que llevan pendientes más del plazo de
 * resolución. Convierte el flag de "vencida" (que hoy solo se calcula al leer el panel) en un aviso
 * proactivo para que la plataforma no deje solicitudes olvidadas.
 */
public interface EscalarSolicitudesVencidas {

	/** Detecta las solicitudes vencidas y las escala (alerta operable). Devuelve cuántas había. */
	int ejecutar();
}
