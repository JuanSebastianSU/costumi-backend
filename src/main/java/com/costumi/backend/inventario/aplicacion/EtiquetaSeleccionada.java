package com.costumi.backend.inventario.aplicacion;

import java.util.UUID;

/** Una etiqueta elegida para una prenda: qué valor se asignó a qué dimensión (tipo). */
public record EtiquetaSeleccionada(UUID tipoEtiquetaId, UUID valorEtiquetaId) {
}
