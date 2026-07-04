package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Usuario;

/**
 * Puerto de salida: emite un token de acceso para un Usuario. Lo implementa un adaptador
 * (JWT) en salida; la aplicación no conoce la tecnología del token.
 */
public interface EmisorDeTokens {

	String emitir(Usuario usuario);
}
