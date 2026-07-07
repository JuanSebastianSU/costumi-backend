package com.costumi.backend.identidad.aplicacion;

import java.util.UUID;

/**
 * Datos de entrada para el auto-registro / solicitud de tienda de Empresa (RF-15.2).
 * {@code ubicacion}, {@code contacto} y {@code solicitanteId} son opcionales (el registro clásico
 * sin cliente los deja en null).
 */
public record RegistrarEmpresaComando(String nombre, String ubicacion, String contacto, UUID solicitanteId) {

	/** Registro clásico solo con nombre (sin datos de solicitud del marketplace). */
	public RegistrarEmpresaComando(String nombre) {
		this(nombre, null, null, null);
	}
}
