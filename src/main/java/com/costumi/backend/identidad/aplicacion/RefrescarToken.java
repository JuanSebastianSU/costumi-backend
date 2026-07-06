package com.costumi.backend.identidad.aplicacion;

/** Puerto de entrada: renueva el acceso a partir de un token de refresco válido (RF-1.1). */
public interface RefrescarToken {

	Credenciales ejecutar(String refreshToken);
}
