package com.costumi.backend.disfraces.dominio;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Slot / Sección de un disfraz (RF-2.3, Capa 3). Lleva el eje de prenda + opcionalidad:
 * <ul>
 *   <li>Eje de prenda: {@link EjeDePrenda#FIJA} (una {@code prendaFijaId} concreta) o
 *       {@link EjeDePrenda#PERSONALIZABLE} (el cliente elige entre varias opciones).</li>
 *   <li>{@code opcional}: el cliente puede omitir la sección; los opcionales no bloquean disponibilidad.</li>
 * </ul>
 *
 * <p>Un slot personalizable define sus opciones de una de dos formas, excluyentes:
 * <ul>
 *   <li><b>Opciones explícitas</b> ({@code prendasOpcion}): el dueño elige a mano prendas concretas de
 *       todo su inventario (sin obligar a una categoría). Es la forma que usa el alta del dueño.</li>
 *   <li><b>Pool</b> ({@link PoolDeSlot}): las opciones se derivan de una categoría + valores de etiqueta
 *       permitidos. Se conserva por compatibilidad.</li>
 * </ul>
 *
 * <p>La <b>talla</b> no es un eje aparte: se modela como una etiqueta (p. ej. el tipo "Talla"). Para
 * fijar una talla con pool se restringe a ese valor; con opciones explícitas, se eligen esas prendas.
 */
public final class Slot {

	private final int orden;
	private final String nombre;
	private final EjeDePrenda ejePrenda;
	private final UUID prendaFijaId;
	private final PoolDeSlot pool;
	private final List<UUID> prendasOpcion;
	private final boolean opcional;

	private Slot(int orden, String nombre, EjeDePrenda ejePrenda, UUID prendaFijaId, PoolDeSlot pool,
			List<UUID> prendasOpcion, boolean opcional) {
		this.orden = exigirOrden(orden);
		this.nombre = exigirNombre(nombre);
		this.ejePrenda = Objects.requireNonNull(ejePrenda, "ejePrenda");
		this.prendaFijaId = prendaFijaId;
		this.pool = pool;
		this.prendasOpcion = (prendasOpcion == null) ? List.of() : List.copyOf(prendasOpcion);
		this.opcional = opcional;
		validarEjePrenda();
	}

	/** Slot con prenda fija (siempre la misma prenda). */
	public static Slot conPrendaFija(int orden, String nombre, UUID prendaFijaId, boolean opcional) {
		return new Slot(orden, nombre, EjeDePrenda.FIJA,
				Objects.requireNonNull(prendaFijaId, "prendaFijaId"), null, List.of(), opcional);
	}

	/** Slot personalizable definido por un pool (categoría + etiquetas permitidas). */
	public static Slot personalizable(int orden, String nombre, PoolDeSlot pool, boolean opcional) {
		return new Slot(orden, nombre, EjeDePrenda.PERSONALIZABLE, null,
				Objects.requireNonNull(pool, "pool"), List.of(), opcional);
	}

	/** Slot personalizable definido por opciones de prenda explícitas (el dueño las elige del inventario). */
	public static Slot personalizableConOpciones(int orden, String nombre, List<UUID> prendasOpcion, boolean opcional) {
		return new Slot(orden, nombre, EjeDePrenda.PERSONALIZABLE, null, null,
				Objects.requireNonNull(prendasOpcion, "prendasOpcion"), opcional);
	}

	public static Slot rehidratar(int orden, String nombre, EjeDePrenda ejePrenda, UUID prendaFijaId,
			PoolDeSlot pool, List<UUID> prendasOpcion, boolean opcional) {
		return new Slot(orden, nombre, ejePrenda, prendaFijaId, pool, prendasOpcion, opcional);
	}

	/** ¿Se puede cubrir este slot? (hay stock para la prenda fija, alguna opción, o el pool). */
	public boolean puedeCubrirse(ConsultaDeStockDePool consulta) {
		return switch (ejePrenda) {
			case FIJA -> consulta.prendaTieneStock(prendaFijaId);
			case PERSONALIZABLE -> tieneOpcionesExplicitas()
					? prendasOpcion.stream().anyMatch(consulta::prendaTieneStock)
					: consulta.poolTieneStock(pool.categoriaId(), pool.etiquetasPermitidas());
		};
	}

	public boolean esObligatorio() {
		return !opcional;
	}

	/** ¿El slot personalizable define sus opciones a mano (en vez de por pool)? */
	public boolean tieneOpcionesExplicitas() {
		return !prendasOpcion.isEmpty();
	}

	private void validarEjePrenda() {
		if (ejePrenda == EjeDePrenda.FIJA) {
			if (prendaFijaId == null || pool != null || !prendasOpcion.isEmpty()) {
				throw new IllegalArgumentException(
						"Un slot de prenda fija requiere prendaFijaId y no lleva pool ni opciones");
			}
			return;
		}
		// PERSONALIZABLE: sin prenda fija y con exactamente una fuente de opciones (explícitas o pool).
		if (prendaFijaId != null) {
			throw new IllegalArgumentException("Un slot personalizable no lleva prenda fija");
		}
		boolean tienePool = pool != null;
		boolean tieneOpciones = !prendasOpcion.isEmpty();
		if (tienePool == tieneOpciones) {
			throw new IllegalArgumentException(
					"Un slot personalizable requiere exactamente una fuente de opciones: prendas explícitas o un pool");
		}
	}

	private static int exigirOrden(int orden) {
		if (orden < 1) {
			throw new IllegalArgumentException("El orden del slot debe ser mayor o igual a 1");
		}
		return orden;
	}

	private static String exigirNombre(String nombre) {
		if (nombre == null || nombre.isBlank()) {
			throw new IllegalArgumentException("El nombre del slot es obligatorio");
		}
		return nombre.trim();
	}

	public int orden() {
		return orden;
	}

	public String nombre() {
		return nombre;
	}

	public EjeDePrenda ejePrenda() {
		return ejePrenda;
	}

	public UUID prendaFijaId() {
		return prendaFijaId;
	}

	public PoolDeSlot pool() {
		return pool;
	}

	/** Opciones de prenda explícitas del slot personalizable (vacío si se define por pool o es fijo). */
	public List<UUID> prendasOpcion() {
		return prendasOpcion;
	}

	public boolean opcional() {
		return opcional;
	}
}
