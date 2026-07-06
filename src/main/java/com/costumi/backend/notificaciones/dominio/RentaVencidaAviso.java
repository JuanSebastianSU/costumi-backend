package com.costumi.backend.notificaciones.dominio;

import java.time.LocalDate;
import java.util.UUID;

/** Datos mínimos de una renta vencida para avisar a su cliente (RF-11.1). Modelo de lectura. */
public record RentaVencidaAviso(UUID rentaId, UUID clienteId, LocalDate fechaDevolucion) {
}
