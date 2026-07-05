package com.costumi.backend.inventario.adaptadores.salida;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Mapeo JPA del Grupo de stock. Lleva {@code empresa_id} (tenant) y la prenda. */
@Entity
@Table(name = "grupo_de_stock")
class GrupoDeStockJpaEntity {

	@Id
	private UUID id;

	@Column(name = "empresa_id", nullable = false)
	private UUID empresaId;

	@Column(name = "prenda_id", nullable = false)
	private UUID prendaId;

	@Column(length = 160)
	private String etiqueta;

	@Column(nullable = false)
	private int disponibles;

	@Column(nullable = false)
	private int danadas;

	@Column(name = "en_limpieza", nullable = false)
	private int enLimpieza;

	@Column(nullable = false)
	private int perdidas;

	protected GrupoDeStockJpaEntity() {
		// requerido por JPA
	}

	GrupoDeStockJpaEntity(UUID id, UUID empresaId, UUID prendaId, String etiqueta,
			int disponibles, int danadas, int enLimpieza, int perdidas) {
		this.id = id;
		this.empresaId = empresaId;
		this.prendaId = prendaId;
		this.etiqueta = etiqueta;
		this.disponibles = disponibles;
		this.danadas = danadas;
		this.enLimpieza = enLimpieza;
		this.perdidas = perdidas;
	}

	UUID getId() {
		return id;
	}

	UUID getEmpresaId() {
		return empresaId;
	}

	UUID getPrendaId() {
		return prendaId;
	}

	String getEtiqueta() {
		return etiqueta;
	}

	int getDisponibles() {
		return disponibles;
	}

	int getDanadas() {
		return danadas;
	}

	int getEnLimpieza() {
		return enLimpieza;
	}

	int getPerdidas() {
		return perdidas;
	}
}
