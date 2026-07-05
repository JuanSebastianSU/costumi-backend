package com.costumi.backend.inventario.dominio;

import java.util.Objects;
import java.util.UUID;

/**
 * Grupo de stock (variante) de una Prenda (RF-2.2): cuenta las unidades con su desglose de
 * estado (disponibles / dañadas / en limpieza / perdidas). Los movimientos entre estados
 * materializan las transiciones de RF-2.11 y alimentan el tablero de RF-9.3.
 *
 * <p>Nota: la combinación exacta de valores de etiqueta que define la variante (RF-2.7.3) se
 * modelará en un slice posterior; por ahora se guarda una {@code etiqueta} descriptiva opcional.
 */
public class GrupoDeStock {

	private final UUID id;
	private final UUID empresaId;
	private final UUID prendaId;
	private String etiqueta;
	private int disponibles;
	private int danadas;
	private int enLimpieza;
	private int perdidas;

	private GrupoDeStock(UUID id, UUID empresaId, UUID prendaId, String etiqueta,
			int disponibles, int danadas, int enLimpieza, int perdidas) {
		this.id = Objects.requireNonNull(id, "id");
		this.empresaId = Objects.requireNonNull(empresaId, "empresaId");
		this.prendaId = Objects.requireNonNull(prendaId, "prendaId");
		this.etiqueta = (etiqueta == null || etiqueta.isBlank()) ? null : etiqueta.trim();
		this.disponibles = exigirNoNegativo(disponibles);
		this.danadas = exigirNoNegativo(danadas);
		this.enLimpieza = exigirNoNegativo(enLimpieza);
		this.perdidas = exigirNoNegativo(perdidas);
	}

	public static GrupoDeStock crear(UUID empresaId, UUID prendaId, String etiqueta, int cantidadInicial) {
		return new GrupoDeStock(UUID.randomUUID(), empresaId, prendaId, etiqueta, cantidadInicial, 0, 0, 0);
	}

	public static GrupoDeStock rehidratar(UUID id, UUID empresaId, UUID prendaId, String etiqueta,
			int disponibles, int danadas, int enLimpieza, int perdidas) {
		return new GrupoDeStock(id, empresaId, prendaId, etiqueta, disponibles, danadas, enLimpieza, perdidas);
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

	public String etiqueta() {
		return etiqueta;
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
