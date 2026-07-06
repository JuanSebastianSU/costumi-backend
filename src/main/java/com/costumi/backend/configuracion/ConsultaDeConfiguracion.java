package com.costumi.backend.configuracion;

import java.util.UUID;

/**
 * API pública de Configuración para que otros módulos consulten los interruptores de la empresa y
 * <b>los respeten de verdad</b> (RF-12.4). P. ej. Notificaciones no avisa multas si el módulo de
 * multas está apagado (RF-6.6).
 */
public interface ConsultaDeConfiguracion {

	/** ¿El módulo de multas está activo para la empresa? (por defecto sí). */
	boolean multasActivas(UUID empresaId);
}
