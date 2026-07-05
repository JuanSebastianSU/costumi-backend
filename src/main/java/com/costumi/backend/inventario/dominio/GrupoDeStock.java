package com.costumi.backend.inventario.dominio;

import java.util.Objects;
import java.util.UUID;

/**
 * Grupo de stock (variante) de una Prenda (RF-2.2): cuenta las unidades con su desglose de
 * estado (disponibles / dañadas / en limpieza / perdidas). Los movimientos entre estados
 * materializan las transiciones de RF-2.11 y alimentan el tablero de RF-9.3.
 *
 * <p>La <b>variante</b> queda definida por su {@link CombinacionDeVariante} (RF-2.7.3): la
 * combinación de valores de etiqueta (p. ej. Color=Rojo, Talla=M). Una combinación vacía es la
 * variante única de una prenda sin dimensiones. Dos grupos de la misma prenda no pueden compartir
 * combinación (lo garantiza el caso de uso al comparar con {@link #mismaVariante(GrupoDeStock)}).
 */
public class GrupoDeStock {

	private final UUID id;
	private final UUID empresaId;
	private final UUID prendaId;
	private final CombinacionDeVariante combinacion;
	private int disponibles;
	private int danadas;
	private int enLimpieza;
	private int perdidas;

	private GrupoDeStock(UUID id, UUID empresaId, UUID prendaId, CombinacionDeVariante combinacion,
			int disponibles, int danadas, int enLimpieza, int perdidas) {
		this.id = Objects.requireNonNull(id, "id");
		this.empresaId = Objects.requireNonNull(empresaId, "empresaId");
		this.prendaId = Objects.requireNonNull(prendaId, "prendaId");
		this.combinacion = Objects.requireNonNull(combinacion, "combinacion");
		this.disponibles = exigirNoNegativo(disponibles);
		this.danadas = exigirNoNegativo(danadas);
		this.enLimpieza = exigirNoNegativo(enLimpieza);
		this.perdidas = exigirNoNegativo(perdidas);
	}

	public static GrupoDeStock crear(UUID empresaId, UUID prendaId, CombinacionDeVariante combinacion,
			int cantidadInicial) {
		return new GrupoDeStock(UUID.randomUUID(), empresaId, prendaId, combinacion, cantidadInicial, 0, 0, 0);
	}

	public static GrupoDeStock rehidratar(UUID id, UUID empresaId, UUID prendaId, CombinacionDeVariante combinacion,
			int disponibles, int danadas, int enLimpieza, int perdidas) {
		return new GrupoDeStock(id, empresaId, prendaId, combinacion, disponibles, danadas, enLimpieza, perdidas);
	}

	/** ¿Este grupo representa la misma variante que {@code otro} (misma combinación de etiquetas)? */
	public boolean mismaVariante(GrupoDeStock otro) {
		return combinacion.equals(otro.combinacion);
	}

	/** Mueve {@code cantidad} unidades de un estado a otro (RF-2.11). */
	public void mover(EstadoUnidad desde, EstadoUnidad hacia, int cantidad) {
		if (cantidad <= 0) {
			throw new IllegalArgumentException("La cantidad a mover debe ser mayor a 0");
		}
		if (desde == hacia) {
			throw new IllegalArgumentException("El estado de origen y destino deben ser distintos");
		}
		int actual = contar(desde);
		if (actual < cantidad) {
			throw new IllegalArgumentException("No hay suficientes unidades en estado " + desde);
		}
		fijar(desde, actual - cantidad);
		fijar(hacia, contar(hacia) + cantidad);
	}

	public int total() {
		return disponibles + danadas + enLimpieza + perdidas;
	}

	public int contar(EstadoUnidad estado) {
		return switch (estado) {
			case DISPONIBLE -> disponibles;
			case DANADA -> danadas;
			case EN_LIMPIEZA -> enLimpieza;
			case PERDIDA -> perdidas;
		};
	}

	private void fijar(EstadoUnidad estado, int valor) {
		switch (estado) {
			case DISPONIBLE -> disponibles = valor;
			case DANADA -> danadas = valor;
			case EN_LIMPIEZA -> enLimpieza = valor;
			case PERDIDA -> perdidas = valor;
		}
	}

	private static int exigirNoNegativo(int valor) {
		if (valor < 0) {
			throw new IllegalArgumentException("Las cantidades de stock no pueden ser negativas");
		}
		return valor;
	}

	public UUID id() {
		return id;
	}

	public UUID empresaId() {
		return empresaId;
	}

	public UUID prendaId() {
		return prendaId;
	}

	public CombinacionDeVariante combinacion() {
		return combinacion;
	}

	public int disponibles() {
		return disponibles;
	}

	public int danadas() {
		return danadas;
	}

	public int enLimpieza() {
		return enLimpieza;
	}

	public int perdidas() {
		return perdidas;
	}
}
