package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Usuario;

/**
 * Puerto de salida: emite un token de acceso para un Usuario. Lo implementa un adaptador
 * (JWT) en salida; la aplicación no conoce la tecnología del token.
 */
public interface EmisorDeTokens {

	/** Token de acceso (corta vida) para el Usuario. */
	String emitir(Usuario usuario);

	/**
	 * Token de refresco (larga vida) con el {@code jti} indicado, para poder registrarlo y revocarlo
	 * server-side (C2). Devuelve el token y su vencimiento (que la app persiste junto al {@code jti}).
	 */
	RefreshEmitido emitirRefresh(Usuario usuario, String jti);
}
