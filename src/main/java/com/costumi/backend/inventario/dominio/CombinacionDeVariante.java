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
 * Combinación de valores de etiqueta que <b>define una variante</b> de stock (RF-2.7.3/2.7.4).
 *
 * <p>Es un <i>value object</i> inmutable: un mapa {@code tipoEtiquetaId -> valorEtiquetaId} donde
 * cada dimensión (tipo) aparece a lo sumo una vez (no tiene sentido "Color=Rojo y Color=Azul" en
 * la misma variante). Dos combinaciones con las mismas selecciones son <b>iguales sin importar el
 * orden</b> ({@code equals}/{@code hashCode} sobre el mapa), lo que permite:
 * <ul>
 *   <li>impedir <b>variantes duplicadas</b> en una misma prenda (uniqueness por combinación), y</li>
 *   <li>la <b>resolución pool→variante→stock</b>: dada una selección deseada, se localiza el grupo
 *       de stock cuya combinación coincide.</li>
 * </ul>
 *
 * <p>Una combinación <b>vacía</b> representa la variante única de una prenda sin dimensiones de
 * variante (una prenda simple con un solo grupo de stock).
 *
 * <p>Solo se guardan combinaciones <b>reales</b> (las que el usuario crea explícitamente); nunca se
 * genera el producto cartesiano de todos los valores.
 */
public final class CombinacionDeVariante {

	private final Map<UUID, UUID> valores;

	private CombinacionDeVariante(Map<UUID, UUID> valores) {
		this.valores = valores;
	}

	/**
	 * Construye la combinación a partir de la selección {@code tipoEtiquetaId -> valorEtiquetaId}.
	 * Rechaza tipos o valores nulos. La copia defensiva preserva el orden de inserción.
	 */
	public static CombinacionDeVariante de(Map<UUID, UUID> seleccion) {
		Objects.requireNonNull(seleccion, "selección");
		Map<UUID, UUID> copia = new LinkedHashMap<>();
		seleccion.forEach((tipo, valor) -> {
			if (tipo == null || valor == null) {
				throw new IllegalArgumentException("Cada selección de variante requiere tipo y valor de etiqueta");
			}
			copia.put(tipo, valor);
		});
		return new CombinacionDeVariante(Collections.unmodifiableMap(copia));
	}

	/** Variante única de una prenda sin dimensiones de variante. */
	public static CombinacionDeVariante unica() {
		return new CombinacionDeVariante(Map.of());
	}

	/** Selección inmutable {@code tipoEtiquetaId -> valorEtiquetaId}. */
	public Map<UUID, UUID> valores() {
		return valores;
	}

	/** ¿Es la variante única (sin dimensiones)? */
	public boolean esUnica() {
		return valores.isEmpty();
	}

	/** Tipos de etiqueta (dimensiones) que participan en esta variante. */
	public Set<UUID> tipos() {
		return valores.keySet();
	}

	/** Valor elegido para una dimensión, si la combinación la incluye. */
	public Optional<UUID> valorDe(UUID tipoEtiquetaId) {
		return Optional.ofNullable(valores.get(tipoEtiquetaId));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		return o instanceof CombinacionDeVariante otra && valores.equals(otra.valores);
	}

	@Override
	public int hashCode() {
		return valores.hashCode();
	}

	@Override
	public String toString() {
		if (valores.isEmpty()) {
			return "CombinacionDeVariante(única)";
		}
		return valores.entrySet().stream()
				.map(e -> e.getKey() + "=" + e.getValue())
				.collect(Collectors.joining(", ", "CombinacionDeVariante(", ")"));
	}
}
