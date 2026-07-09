package com.costumi.backend.disfraces.adaptadores.salida;

import com.costumi.backend.disfraces.dominio.EjeDePrenda;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Mapeo JPA de un slot del disfraz, con su pool de etiquetas permitidas (si es personalizable). */
@Entity
@Table(name = "disfraz_slot")
class DisfrazSlotJpaEntity {

	@Id
	private UUID id;

	@Column(name = "disfraz_id", nullable = false)
	private UUID disfrazId;

	@Column(nullable = false)
	private int orden;

	@Column(nullable = false, length = 120)
	private String nombre;

	@Enumerated(EnumType.STRING)
	@Column(name = "eje_prenda", nullable = false, length = 16)
	private EjeDePrenda ejePrenda;

	@Column(name = "prenda_fija_id")
	private UUID prendaFijaId;

	@Column(name = "categoria_id")
	private UUID categoriaId;

	@Column(nullable = false)
	private boolean opcional;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "disfraz_slot_etiqueta", joinColumns = @JoinColumn(name = "slot_id"))
	private Set<EtiquetaDeSlotEmbeddable> etiquetasPermitidas = new LinkedHashSet<>();

	protected DisfrazSlotJpaEntity() {
		// requerido por JPA
	}

	DisfrazSlotJpaEntity(UUID id, UUID disfrazId, int orden, String nombre, EjeDePrenda ejePrenda,
			UUID prendaFijaId, UUID categoriaId, boolean opcional,
			Set<EtiquetaDeSlotEmbeddable> etiquetasPermitidas) {
		this.id = id;
		this.disfrazId = disfrazId;
		this.orden = orden;
		this.nombre = nombre;
		this.ejePrenda = ejePrenda;
		this.prendaFijaId = prendaFijaId;
		this.categoriaId = categoriaId;
		this.opcional = opcional;
		this.etiquetasPermitidas = etiquetasPermitidas;
	}

	UUID getId() {
		return id;
	}

	UUID getDisfrazId() {
		return disfrazId;
	}

	int getOrden() {
		return orden;
	}

	String getNombre() {
		return nombre;
	}

	EjeDePrenda getEjePrenda() {
		return ejePrenda;
	}

	UUID getPrendaFijaId() {
		return prendaFijaId;
	}

	UUID getCategoriaId() {
		return categoriaId;
	}

	boolean isOpcional() {
		return opcional;
	}

	Set<EtiquetaDeSlotEmbeddable> getEtiquetasPermitidas() {
		return etiquetasPermitidas;
	}
}
