package com.costumi.backend.identidad.aplicacion;

/** Resultado de una autenticación: el token de acceso emitido. */
public record Credenciales(String accessToken) {
}
