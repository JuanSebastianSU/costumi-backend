package com.costumi.backend.identidad.aplicacion;

import java.time.Instant;
import java.util.UUID;

/** Vista de lectura de una solicitud de Empresa pendiente, con su marca de vencida (RF-15.4). */
public record EmpresaPendienteVista(UUID id, String nombre, Instant fechaRegistro, boolean vencida) {
}
