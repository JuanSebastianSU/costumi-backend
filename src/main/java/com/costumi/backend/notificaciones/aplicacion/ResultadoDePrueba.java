package com.costumi.backend.notificaciones.aplicacion;

/**
 * Resultado de una push de prueba: si salió y, si no, el motivo tal como lo devolvió el proveedor.
 *
 * <p>El envío normal se traga los errores a propósito —un aviso que no sale no debe romper el flujo—,
 * así que sin esto la única pista queda en los logs del servidor.
 */
public record ResultadoDePrueba(boolean enviado, String detalle) {
}
