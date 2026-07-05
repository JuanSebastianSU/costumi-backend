package com.costumi.backend.inventario.aplicacion;

import java.util.UUID;

/** Una selección de la combinación de variante: qué valor se eligió para qué dimensión (tipo). */
public record SeleccionVariante(UUID tipoEtiquetaId, UUID valorEtiquetaId) {
}
