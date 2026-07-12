package com.costumi.backend.identidad.aplicacion;

/** Puerto de entrada: cierra la sesión revocando la familia del refresco presentado (logout, C2). */
public interface CerrarSesion {

	/** Revoca toda la cadena de refrescos de esa sesión. Idempotente: un token inválido/vencido no falla. */
	void cerrar(String refreshToken);
}
