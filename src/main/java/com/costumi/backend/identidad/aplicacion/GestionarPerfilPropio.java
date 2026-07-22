package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Usuario;

import java.util.UUID;

/**
 * Puerto de entrada: lo que el usuario puede hacer sobre <b>su propia</b> cuenta (RF-14), sin depender
 * de que un dueño se lo administre.
 *
 * <p>Todas las operaciones reciben el id del usuario autenticado: nadie edita el perfil de otro. Cambiar
 * el correo no está incluido a propósito — identifica la cuenta y cambiarlo es otra historia (requiere
 * verificar el correo nuevo).
 */
public interface GestionarPerfilPropio {

	/** Los datos de la cuenta autenticada. */
	Usuario verPerfil(UUID usuarioId);

	/** Actualiza nombre y teléfono. Vacío borra el dato: son opcionales. */
	Usuario actualizarPerfil(UUID usuarioId, String nombre, String telefono);

	/**
	 * Cambia la contraseña estando dentro de la sesión. Exige la actual: sin eso, quien se siente frente a
	 * una sesión abierta podría dejar al dueño fuera de su propia cuenta.
	 */
	void cambiarContrasena(UUID usuarioId, String contrasenaActual, String contrasenaNueva);
}
