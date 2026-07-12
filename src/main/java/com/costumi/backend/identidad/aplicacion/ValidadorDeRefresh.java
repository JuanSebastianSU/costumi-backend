package com.costumi.backend.identidad.aplicacion;

/** Puerto de salida: valida un token de refresco y devuelve el email + {@code jti} de su dueño (RF-1.1, C2). */
public interface ValidadorDeRefresh {

	/** Email y {@code jti} si el refresh es válido (firma, vigencia, tipo y jti presente); si no, lanza {@link RefreshInvalido}. */
	RefreshDecodificado validar(String refreshToken);
}
