package com.costumi.backend.identidad.adaptadores.entrada;

/** DTO de salida del login/refresh: token de acceso, token de refresco y tipo (RF-1.1). */
public record TokenResponse(String accessToken, String refreshToken, String tokenType) {
}
