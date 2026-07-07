package com.costumi.backend.identidad.aplicacion;

import java.time.Instant;
import java.util.UUID;

/**
 * Vista de lectura de una solicitud de Empresa pendiente para el panel del SuperAdmin: incluye los
 * datos de la solicitud (ubicación/contacto) y el cliente solicitante, además de la marca de vencida.
 */
public record EmpresaPendienteVista(UUID id, String nombre, Instant fechaRegistro, boolean vencida,
		String ubicacion, String contacto, UUID solicitanteId) {
}
