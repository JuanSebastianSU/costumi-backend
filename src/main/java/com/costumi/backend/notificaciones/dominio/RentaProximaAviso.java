package com.costumi.backend.notificaciones.dominio;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Datos de una renta ACTIVA próxima a vencer, para recordarle al cliente que devuelva a tiempo
 * (RF-11.1). Lleva la sucursal para poder incluir dirección/ubicación en el mensaje. Modelo de lectura.
 */
public record RentaProximaAviso(UUID rentaId, UUID clienteId, UUID sucursalId, LocalDate fechaDevolucion) {
}
