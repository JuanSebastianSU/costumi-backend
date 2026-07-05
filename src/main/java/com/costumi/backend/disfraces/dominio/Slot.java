package com.costumi.backend.disfraces.dominio;

import java.util.Objects;
import java.util.UUID;

/**
 * Slot / Sección de un disfraz por partes (RF-2.3, Capa 3). Lleva los <b>dos ejes</b> + opcionalidad:
 * <ul>
 *   <li>Eje de talla: {@link EjeDeTalla#FIJA} (se escribe {@code tallaFija}) o {@link EjeDeTalla#LIBRE}.</li>
 *   <li>Eje de prenda: {@link EjeDePrenda#FIJA} (una {@code prendaFijaId} concreta) o
 *       {@link EjeDePrenda#PERSONALIZABLE} (el cliente elige del {@link PoolDeSlot}).</li>
 *   <li>{@code opcional}: el cliente puede omitir la sección; los opcionales no bloquean disponibilidad.</li>
 * </ul>
 */
public final class Slot {

	private final int orden;
	private final String nombre;
	private final EjeDeTalla ejeTalla;
	private final String tallaFija;
	private final EjeDePrenda ejePrenda;
	private final UUID prendaFijaId;
	private final PoolDeSlot pool;
	private final boolean opcional;

	private Slot(int orden, String nombre, EjeDeTalla ejeTalla, String tallaFija, EjeDePrenda ejePrenda,
			UUID prendaFijaId, PoolDeSlot pool, boolean opcional) {
		this.orden = exigirOrden(orden);
		this.nombre = exigirNombre(nombre);
		this.ejeTalla = Objects.requireNonNull(ejeTalla, "ejeTalla");
		this.tallaFija = validarTalla(ejeTalla, tallaFija);
		this.ejePrenda = Objects.requireNonNull(ejePrenda, "ejePrenda");
		this.prendaFijaId = prendaFijaId;
		this.pool = pool;
		this.opcional = opcional;
		validarEjePrenda();
	}

	/** Slot con prenda fija (siempre la misma prenda). */
	public static Slot conPrendaFija(int orden, String nombre, EjeDeTalla ejeTalla, String tallaFija,
			UUID prendaFijaId, boolean opcional) {
		return new Slot(orden, nombre, ejeTalla, tallaFija, EjeDePrenda.FIJA,
				Objects.requireNonNull(prendaFijaId, "prendaFijaId"), null, opcional);
	}

	/** Slot personalizable: el cliente elige una prenda del pool. */
	public static Slot personalizable(int orden, String nombre, EjeDeTalla ejeTalla, String tallaFija,
			PoolDeSlot pool, boolean opcional) {
		return new Slot(orden, nombre, ejeTalla, tallaFija, EjeDePrenda.PERSONALIZABLE, null,
				Objects.requireNonNull(pool, "pool"), opcional);
	}

	public static Slot rehidratar(int orden, String nombre, EjeDeTalla ejeTalla, String tallaFija,
			EjeDePrenda ejePrenda, UUID prendaFijaId, PoolDeSlot pool, boolean opcional) {
		return new Slot(orden, nombre, ejeTalla, tallaFija, ejePrenda, prendaFijaId, pool, opcional);
	}

	/** ¿Se puede cubrir este slot? (hay stock para la prenda fija, o el pool tiene stock). */
	public boolean puedeCubrirse(ConsultaDeStockDePool consulta) {
		return switch (ejePrenda) {
			case FIJA -> consulta.prendaTieneStock(prendaFijaId);
			case PERSONALIZABLE -> consulta.poolTieneStock(pool.categoriaId(), pool.etiquetasPermitidas());
		};
	}

	public boolean esObligatorio() {
		return !opcional;
	}

	private void validarEjePrenda() {
		if (ejePrenda == EjeDePrenda.FIJA && (prendaFijaId == null || pool != null)) {
			throw new IllegalArgumentException("Un slot de prenda fija requiere prendaFijaId y no lleva pool");
		}
		if (ejePrenda == EjeDePrenda.PERSONALIZABLE && (pool == null || prendaFijaId != null)) {
			throw new IllegalArgumentException("Un slot personalizable requiere pool y no lleva prenda fija");
		}
	}

	private static String validarTalla(EjeDeTalla ejeTalla, String tallaFija) {
		if (ejeTalla == EjeDeTalla.FIJA) {
			if (tallaFija == null || tallaFija.isBlank()) {
				throw new IllegalArgumentException("Un slot de talla fija requiere la talla");
			}
			return tallaFija.trim();
		}
		return null;
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

	public EjeDeTalla ejeTalla() {
		return ejeTalla;
	}

	public String tallaFija() {
		return tallaFija;
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

	public boolean opcional() {
		return opcional;
	}
}
