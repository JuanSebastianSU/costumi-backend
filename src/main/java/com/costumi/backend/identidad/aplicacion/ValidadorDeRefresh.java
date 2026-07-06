package com.costumi.backend.identidad.aplicacion;

/** Puerto de salida: valida un token de refresco y devuelve el email de su dueño (RF-1.1). */
public interface ValidadorDeRefresh {

	/** Email del usuario si el refresh es válido (firma, vigencia y tipo); si no, lanza {@link RefreshInvalido}. */
	String emailDelRefresh(String refreshToken);
}
