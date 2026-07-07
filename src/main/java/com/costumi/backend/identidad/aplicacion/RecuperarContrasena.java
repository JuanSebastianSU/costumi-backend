package com.costumi.backend.identidad.aplicacion;

/** Puerto de entrada: recuperación de contraseña (RF-1.1). */
public interface RecuperarContrasena {

	/** Genera un token y lo envía por email si el correo existe. No revela si existe o no. */
	void solicitar(String email);

	/** Valida el token (vigente y no usado) y cambia la contraseña del usuario. */
	void restablecer(String token, String nuevaPassword);
}
