package com.costumi.backend.identidad.aplicacion;

/**
 * Puerto de entrada: auto-registro de un usuario final (CLIENTE) del marketplace. Crea la cuenta
 * y devuelve credenciales (auto-login), para que tras registrarse quede la sesión iniciada.
 */
public interface RegistrarCliente {

	Credenciales ejecutar(String email, String password);
}
