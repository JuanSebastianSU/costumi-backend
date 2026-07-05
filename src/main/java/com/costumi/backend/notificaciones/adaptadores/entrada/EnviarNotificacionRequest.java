package com.costumi.backend.notificaciones.adaptadores.entrada;

import com.costumi.backend.notificaciones.dominio.CanalNotificacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** DTO de entrada para enviar una notificación. */
public record EnviarNotificacionRequest(

		UUID clienteId,

		@NotNull(message = "El canal es obligatorio") CanalNotificacion canal,

		@NotBlank(message = "El mensaje es obligatorio") String mensaje) {
}
