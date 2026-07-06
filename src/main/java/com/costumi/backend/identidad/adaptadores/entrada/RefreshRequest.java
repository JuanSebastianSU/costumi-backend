package com.costumi.backend.identidad.adaptadores.entrada;

import jakarta.validation.constraints.NotBlank;

/** DTO de entrada para renovar el acceso con un token de refresco (RF-1.1). */
public record RefreshRequest(@NotBlank(message = "El token de refresco es obligatorio") String refreshToken) {
}
