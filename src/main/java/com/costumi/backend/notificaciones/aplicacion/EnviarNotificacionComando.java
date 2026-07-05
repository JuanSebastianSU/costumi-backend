package com.costumi.backend.notificaciones.aplicacion;

import com.costumi.backend.notificaciones.dominio.CanalNotificacion;

import java.util.UUID;

/** Datos para enviar una notificación a un cliente (RF-11). */
public record EnviarNotificacionComando(UUID empresaId, UUID clienteId, CanalNotificacion canal, String mensaje) {
}
