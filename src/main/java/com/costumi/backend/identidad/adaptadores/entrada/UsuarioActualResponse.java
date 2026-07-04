package com.costumi.backend.identidad.adaptadores.entrada;

/** DTO de salida de la identidad del usuario autenticado (claims del token). */
public record UsuarioActualResponse(String id, String email, String rol, String empresaId) {
}
