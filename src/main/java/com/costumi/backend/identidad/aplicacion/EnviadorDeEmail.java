package com.costumi.backend.identidad.aplicacion;

/** Puerto de salida: envío de un correo (recuperación de contraseña, RF-1.1). */
public interface EnviadorDeEmail {

	void enviar(String destinatario, String asunto, String cuerpo);
}
