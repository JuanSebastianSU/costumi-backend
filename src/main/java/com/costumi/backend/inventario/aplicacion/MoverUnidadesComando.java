package com.costumi.backend.inventario.aplicacion;

import com.costumi.backend.inventario.dominio.EstadoUnidad;

import java.util.UUID;

/** Datos para mover unidades de un estado a otro dentro de un Grupo de stock (RF-2.11). */
public record MoverUnidadesComando(UUID empresaId, UUID grupoId, EstadoUnidad desde, EstadoUnidad hacia,
		int cantidad) {
}
