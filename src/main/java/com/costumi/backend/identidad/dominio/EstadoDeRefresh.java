package com.costumi.backend.identidad.dominio;

/** Estado de un token de refresco dentro de su familia (C2). */
public enum EstadoDeRefresh {

	/** Vigente y aún no usado para rotar. */
	ACTIVO,

	/** Ya se usó para emitir un nuevo par (rotación). Presentarlo de nuevo = reuso (robo). */
	ROTADO,

	/** Anulado (logout o revocación de la familia por reuso). */
	REVOCADO
}
