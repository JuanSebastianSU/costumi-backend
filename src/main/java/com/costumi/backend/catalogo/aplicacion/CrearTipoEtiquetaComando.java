package com.costumi.backend.catalogo.aplicacion;

import java.util.UUID;

/** Datos para crear un Tipo de etiqueta en la empresa del usuario autenticado (RF-2.7.1/2.7.2). */
public record CrearTipoEtiquetaComando(UUID empresaId, String nombre, boolean defineVariante,
		boolean seleccionablePorCliente) {
}
