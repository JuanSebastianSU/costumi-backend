package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Usuario;

/**
 * Puerto de salida: emite un token de acceso para un Usuario. Lo implementa un adaptador
 * (JWT) en salida; la aplicación no conoce la tecnología del token.
 */
public interface EmisorDeTokens {

	/** Token de acceso (corta vida) para el Usuario. */
	String emitir(Usuario usuario);

	/** Token de refresco (larga vida) para renovar el acceso sin volver a loguearse (RF-1.1). */
	String emitirRefresh(Usuario usuario);
}
