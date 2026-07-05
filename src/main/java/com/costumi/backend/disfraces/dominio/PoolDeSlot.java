package com.costumi.backend.disfraces.dominio;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Pool de prendas de un slot personalizable (RF-2.7.5): las prendas elegibles son las de una
 * {@code categoria} cuyas etiquetas satisfacen los <b>valores permitidos</b> por dimensión
 * ({@code tipoEtiquetaId -> valores permitidos}). Un mapa vacío = cualquier prenda de la categoría.
 * Value object inmutable.
 */
public final class PoolDeSlot {

	private final UUID categoriaId;
	private final Map<UUID, Set<UUID>> etiquetasPermitidas;

	private PoolDeSlot(UUID categoriaId, Map<UUID, Set<UUID>> etiquetasPermitidas) {
		this.categoriaId = categoriaId;
		this.etiquetasPermitidas = etiquetasPermitidas;
	}

	public static PoolDeSlot de(UUID categoriaId, Map<UUID, Set<UUID>> etiquetasPermitidas) {
		Objects.requireNonNull(categoriaId, "categoriaId");
		Map<UUID, Set<UUID>> copia = new LinkedHashMap<>();
		if (etiquetasPermitidas != null) {
			etiquetasPermitidas.forEach((tipo, valores) -> {
				if (tipo == null || valores == null || valores.isEmpty()) {
					throw new IllegalArgumentException("Cada dimensión del pool requiere tipo y al menos un valor permitido");
				}
				Set<UUID> copiaValores = new LinkedHashSet<>();
				for (UUID valor : valores) {
					if (valor == null) {
						throw new IllegalArgumentException("Los valores permitidos del pool no pueden ser nulos");
					}
					copiaValores.add(valor);
				}
				copia.put(tipo, Collections.unmodifiableSet(copiaValores));
			});
		}
		return new PoolDeSlot(categoriaId, Collections.unmodifiableMap(copia));
	}

	public UUID categoriaId() {
		return categoriaId;
	}

	public Map<UUID, Set<UUID>> etiquetasPermitidas() {
		return etiquetasPermitidas;
	}
}
