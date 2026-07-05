package com.costumi.backend.inventario.dominio;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Valores de etiqueta que <b>clasifican una Prenda</b> (RF-2.7, Capa 2): "una categoría + valores
 * de etiqueta". Es un <i>value object</i> inmutable: un mapa {@code tipoEtiquetaId -> valorEtiquetaId}
 * con cada dimensión (tipo) a lo sumo una vez.
 *
 * <p>Se guardan solo <b>referencias por id</b> a la taxonomía (nunca el texto), de modo que renombrar
 * un valor/tipo <b>propaga</b> a las prendas sin tocarlas (RF-2.7.6).
 *
 * <p>Distinto de {@link CombinacionDeVariante}: aquí las etiquetas describen/clasifican el ítem de la
 * biblioteca (a nivel de Prenda), mientras que la combinación de variante vive a nivel de grupo de
 * stock y solo abarca los tipos marcados "definen variante". Comparten forma pero no concepto.
 */
public final class EtiquetasDePrenda {

	private final Map<UUID, UUID> valores;

	private EtiquetasDePrenda(Map<UUID, UUID> valores) {
		this.valores = valores;
	}

	/** Construye a partir de la selección {@code tipoEtiquetaId -> valorEtiquetaId}; rechaza nulos. */
	public static EtiquetasDePrenda de(Map<UUID, UUID> seleccion) {
		Objects.requireNonNull(seleccion, "selección");
		Map<UUID, UUID> copia = new LinkedHashMap<>();
		seleccion.forEach((tipo, valor) -> {
			if (tipo == null || valor == null) {
				throw new IllegalArgumentException("Cada etiqueta de la prenda requiere tipo y valor");
			}
			copia.put(tipo, valor);
		});
		return new EtiquetasDePrenda(Collections.unmodifiableMap(copia));
	}

	/** Prenda sin etiquetas (ítem sin clasificar). */
	public static EtiquetasDePrenda ninguna() {
		return new EtiquetasDePrenda(Map.of());
	}

	public Map<UUID, UUID> valores() {
		return valores;
	}

	public boolean esVacia() {
		return valores.isEmpty();
	}

	public Set<UUID> tipos() {
		return valores.keySet();
	}

	public Optional<UUID> valorDe(UUID tipoEtiquetaId) {
		return Optional.ofNullable(valores.get(tipoEtiquetaId));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		return o instanceof EtiquetasDePrenda otras && valores.equals(otras.valores);
	}

	@Override
	public int hashCode() {
		return valores.hashCode();
	}

	@Override
	public String toString() {
		if (valores.isEmpty()) {
			return "EtiquetasDePrenda(ninguna)";
		}
		return valores.entrySet().stream()
				.map(e -> e.getKey() + "=" + e.getValue())
				.collect(Collectors.joining(", ", "EtiquetasDePrenda(", ")"));
	}
}
