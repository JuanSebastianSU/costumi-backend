package com.costumi.backend.identidad.aplicacion;

/** Datos extraídos de un refresh válido: el email de su dueño y el {@code jti} para ubicarlo server-side (C2). */
public record RefreshDecodificado(String email, String jti) {
}
