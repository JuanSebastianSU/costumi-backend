package com.costumi.backend.identidad.aplicacion;

/** Puerto de entrada: autentica por email + contraseña y devuelve un token (RF-1.1, RF-17.4). */
public interface AutenticarUsuario {

	Credenciales autenticar(String email, String password);
}
