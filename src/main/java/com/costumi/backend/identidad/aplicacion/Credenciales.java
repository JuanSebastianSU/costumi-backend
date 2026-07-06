package com.costumi.backend.identidad.aplicacion;

/** Resultado de una autenticación: el token de acceso y el de refresco emitidos (RF-1.1). */
public record Credenciales(String accessToken, String refreshToken) {
}
