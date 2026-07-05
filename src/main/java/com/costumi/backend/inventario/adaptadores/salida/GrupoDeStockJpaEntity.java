package com.costumi.backend.inventario.adaptadores.salida;

import com.costumi.backend.compartido.FiltroTenant;
import org.hibernate.annotations.Filter;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Mapeo JPA del Grupo de stock. Lleva {@code empresa_id} (tenant), la prenda y su combinación de variante. */
@Entity
@Table(name = "grupo_de_stock")
@Filter(name = FiltroTenant.NOMBRE)
class GrupoDeStockJpaEntity {

	@Id
	private UUID id;

	@Column(name = "empresa_id", nullable = false)
	private UUID empresaId;

	@Column(name = "prenda_id", nullable = false)
	private UUID prendaId;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "grupo_de_stock_valor", joinColumns = @JoinColumn(name = "grupo_id"))
	private Set<ValorDeVarianteEmbeddable> combinacion = new LinkedHashSet<>();

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

	GrupoDeStockJpaEntity(UUID id, UUID empresaId, UUID prendaId, Set<ValorDeVarianteEmbeddable> combinacion,
			int disponibles, int danadas, int enLimpieza, int perdidas) {
		this.id = id;
		this.empresaId = empresaId;
		this.prendaId = prendaId;
		this.combinacion = combinacion;
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

	Set<ValorDeVarianteEmbeddable> getCombinacion() {
		return combinacion;
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
