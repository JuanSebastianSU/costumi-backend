package com.costumi.backend.identidad.aplicacion;

/**
 * Datos extraídos de un refresh válido: el email de su dueño, el {@code jti} para ubicarlo server-side (C2),
 * y el <b>contexto</b> que llevaba el token ({@code empresaId} + {@code rol}) para preservar el modo
 * Comprando/Trabajando al refrescar (Fase B). {@code empresaId} es null en modo compra (cliente).
 */
public record RefreshDecodificado(String email, String jti, String empresaId, String rol) {
}
