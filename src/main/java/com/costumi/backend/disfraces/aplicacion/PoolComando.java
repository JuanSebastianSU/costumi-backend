package com.costumi.backend.disfraces.aplicacion;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Pool de un slot personalizable: categoría + valores de etiqueta permitidos por dimensión. */
public record PoolComando(UUID categoriaId, Map<UUID, Set<UUID>> etiquetasPermitidas) {
}
