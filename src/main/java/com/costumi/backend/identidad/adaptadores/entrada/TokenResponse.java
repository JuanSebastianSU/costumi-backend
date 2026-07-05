package com.costumi.backend.identidad.adaptadores.entrada;

/** DTO de salida del login: el token de acceso y su tipo. */
public record TokenResponse(String accessToken, String tokenType) {
}
