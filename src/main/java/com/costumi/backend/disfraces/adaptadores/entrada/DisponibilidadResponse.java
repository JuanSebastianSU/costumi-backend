package com.costumi.backend.disfraces.adaptadores.entrada;

import java.util.UUID;

/** DTO de salida de la disponibilidad derivada de un disfraz (RF-2.4). */
public record DisponibilidadResponse(UUID disfrazId, boolean disponible) {
}
